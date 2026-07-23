#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Siemens AG
# SPDX-License-Identifier: MIT
"""
Package enabled CIS hardening configuration snippets into a partial root directory
for Debian package creation.

This script:
1. Parses a .config.json file to find rules with apply=true
2. Maps rule names to generated config file snippets
3. Copies enabled config files to the correct locations in a partial root directory
4. Creates the directory structure that dpkg expects for package building

Usage:
    python3 enable-cis-rules.py \\
        --config /path/to/.config.json \\
        --snippets-dir /path/to/cis-config-snippets \\
        --output-dir /path/to/partial/root
"""

import argparse
import logging
import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Set

logger = logging.getLogger(__name__)


def load_json(path: Path, logger: logging.Logger) -> dict:
    """Load a JSON file. On error, log to stderr and exit with status 1."""
    try:
        with Path(path).open() as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError) as exc:
        logger.error(f"Error loading file '{path}': {exc}")
        sys.exit(1)


def get_enabled_rules(config: dict) -> set[str]:
    """Return the set of rule names where rule.apply is true and the name is non-empty."""
    enabled: set[str] = set()
    for entry in config.get("rules", []):
        rule = entry.get("rule", {}) if isinstance(entry, dict) else {}
        if rule.get("apply", False) and rule.get("name"):
            enabled.add(rule["name"])
    return enabled


# ---------------------------------------------------------------------------
# Dynamic rule discovery
# Discovers config files based on CIS rule IDs encoded in filenames
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Destination path prefix overrides
# By default snippets are placed under etc/ in the output root.
# Entries here map a source subdirectory prefix to a different destination
# prefix.  Keys are Path parts (as strings) relative to snippets_dir.
# ---------------------------------------------------------------------------
DEST_PREFIX_OVERRIDES: Dict[str, str] = {
    "pam-configs": "usr/share/pam-configs",
    "chrony": "etc/chrony",
    "configuration": "usr/share/cip-cis-rules/configuration",
    "conf.d-overrides": "usr/share/cip-cis-rules/conf.d-overrides",
    "sshd_config.d": "etc/ssh/sshd_config.d",
}

# ---------------------------------------------------------------------------
# Per-source-prefix file mode overrides.
# By default snippets are written with the umask default (typically 0644).
# Some destinations are rejected by the consuming tool unless mode is set
# explicitly; in particular sudo refuses to read /etc/sudoers.d/* unless
# mode is <= 0440 and owner is root.
# Keys are the first component of the source path (relative to snippets_dir).
# ---------------------------------------------------------------------------
DEST_MODE_OVERRIDES: Dict[str, int] = {
    "sudoers.d": 0o440,
    "configuration": 0o755,
}


def dest_mode(source_rel: "Path") -> "int | None":
    """
    Return the explicit file mode for the destination of ``source_rel``,
    or ``None`` if no override applies (caller should leave the file at the
    umask default).
    """
    parts = source_rel.parts
    first = parts[0] if parts else ""
    return DEST_MODE_OVERRIDES.get(first)


def dest_rel_path(source_rel: "Path") -> "Path":
    """
    Return the destination path relative to the output root for a given
    source-relative path.

    The first component of source_rel is matched against DEST_PREFIX_OVERRIDES.
    If found the first component is replaced with the mapped prefix.
    Otherwise 'etc' is prepended (legacy behaviour).
    """
    parts = source_rel.parts
    first = parts[0] if parts else ""
    if first in DEST_PREFIX_OVERRIDES:
        rest = parts[1:]
        if not rest:
            raise ValueError(
                f"dest_rel_path: snippet '{source_rel}' sits at snippets root "
                f"with a prefix-override key as its name — cannot determine destination"
            )
        return Path(DEST_PREFIX_OVERRIDES[first]) / Path(*rest)
    return Path("etc") / source_rel


def discover_rule_files(
    snippets_dir: Path, logger: logging.Logger
) -> Dict[str, List[Path]]:
    """
    Dynamically discover config files by scanning snippets directory.

    Looks for files matching pattern: cis-X.Y.Z-* or cis-X.Y.Z.W-* (dot-separated rule ID)

    Returns a mapping of rule_id -> list of file paths relative to snippets_dir.
    """
    # Pattern to match CIS rule IDs in filenames (dot-separated)
    pattern = re.compile(r"cis-(\d+)\.(\d+)\.(\d+)(?:\.(\d+))?")

    rule_files: Dict[str, List[Path]] = {}

    # Recursively find all files in snippets directory
    for file_path in snippets_dir.rglob("*"):
        if not file_path.is_file():
            continue

        filename = file_path.name

        # Try to match pattern
        match = pattern.search(filename)
        if match:
            groups = match.groups()

            # Build rule ID (X.Y.Z or X.Y.Z.W format)
            if groups[3]:  # 4-part rule ID
                rule_id = f"{groups[0]}.{groups[1]}.{groups[2]}.{groups[3]}"
            else:  # 3-part rule ID
                rule_id = f"{groups[0]}.{groups[1]}.{groups[2]}"

            # Get relative path from snippets_dir
            rel_path = file_path.relative_to(snippets_dir)

            # Add to mapping
            if rule_id not in rule_files:
                rule_files[rule_id] = []
            rule_files[rule_id].append(rel_path)

            logger.debug(f"Discovered rule {rule_id}: {rel_path}")

    logger.info(f"Discovered {len(rule_files)} rules with config files")
    return rule_files


def get_rule_values(config: dict) -> Dict[str, str]:
    """Extract the optional value field for each rule."""
    values = {}
    for rule_entry in config.get("rules", []):
        rule = rule_entry.get("rule", {})
        name = rule.get("name", "")
        value = rule.get("value")
        if name and value is not None:
            values[name] = value
    return values


def package_configs(
    enabled_rules: Set[str],
    rule_files: Dict[str, List[Path]],
    rule_values: Dict[str, str],
    snippets_dir: Path,
    output_dir: Path,
    logger: logging.Logger,
) -> int:
    """
    Copy enabled config snippets to the partial root directory structure.
    Returns the number of errors encountered.
    """
    errors = 0
    processed_files = 0

    for rule_name in sorted(enabled_rules):
        if rule_name not in rule_files:
            logger.debug(f"SKIP {rule_name}: no config files found for this rule")
            continue

        file_paths = rule_files[rule_name]

        for source_rel in file_paths:
            source_path = snippets_dir / source_rel
            # Use new dest_rel_path function to determine destination
            dest_rel = dest_rel_path(source_rel)
            dest_path = output_dir / dest_rel

            if not source_path.exists():
                logger.error(f"{rule_name}: config file '{source_path}' not found")
                errors += 1
                continue

            logger.debug(f"copy {rule_name}: {source_rel} → {dest_rel}")

            # Create destination directory
            dest_path.parent.mkdir(parents=True, exist_ok=True)
            # Read source, substitute @@VALUE@@ if a value is configured
            content = source_path.read_text()
            if "@@VALUE@@" in content:
                value = rule_values.get(rule_name)
                if value is None:
                    logger.error(
                        f"{rule_name}: snippet contains @@VALUE@@ but no "
                        f"'value' field is set in .config.json"
                    )
                    errors += 1
                    continue
                content = content.replace("@@VALUE@@", value)
                logger.debug(f"substituted @@VALUE@@={value!r} in {source_rel}")
            dest_path.write_text(content)
            mode = dest_mode(source_rel)
            if mode is not None:
                dest_path.chmod(mode)
                logger.debug(f"set mode {mode:#o} on {dest_rel}")

            processed_files += 1

    skipped_rules = len(enabled_rules) - len(
        [r for r in enabled_rules if r in rule_files]
    )
    logger.debug(
        f"Summary: {processed_files} config files processed, "
        f"{skipped_rules} rules skipped (no config files found)"
    )

    return errors


def setup_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="package-enabled-configs",
        description=(
            "Package enabled CIS hardening configuration snippets into a partial "
            "root directory for Debian package creation."
        ),
    )
    parser.add_argument(
        "--config",
        type=Path,
        required=True,
        metavar="FILE",
        help="Path to .config.json file containing rule selections",
    )
    parser.add_argument(
        "--snippets-dir",
        type=Path,
        required=True,
        metavar="DIR",
        help="Directory containing generated config file snippets",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        required=True,
        metavar="DIR",
        help="Output directory for partial root filesystem structure",
    )
    return parser


def main() -> None:
    parser = setup_parser()
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(levelname)s: %(message)s",
    )

    # Validate inputs
    if not args.config.is_file():
        logger.error(f"config file '{args.config}' does not exist")
        sys.exit(1)

    if not args.snippets_dir.is_dir():
        logger.error(f"snippets directory '{args.snippets_dir}' does not exist")
        sys.exit(1)

    args.output_dir.mkdir(parents=True, exist_ok=True)

    # Discover available config files
    logger.debug(f"Discovering config files in: {args.snippets_dir}")
    rule_files = discover_rule_files(args.snippets_dir, logger)

    # Load configuration and get enabled rules
    config = load_json(args.config, logger)
    enabled_rules = get_enabled_rules(config)
    rule_values = get_rule_values(config)

    logger.debug(f"Loaded configuration from: {args.config}")
    logger.info(f"Found {len(enabled_rules)} enabled rules")
    if enabled_rules:
        implementable_rules = enabled_rules & set(rule_files.keys())
        logger.debug(f"Rules with config files available: {len(implementable_rules)}")

    if not enabled_rules:
        logger.error("No rules are enabled (apply=true) in the configuration")
        sys.exit(1)

    # Package the configs
    errors = package_configs(
        enabled_rules,
        rule_files,
        rule_values,
        args.snippets_dir,
        args.output_dir,
        logger,
    )

    if errors:
        logger.error(f"{errors} error(s) occurred during packaging")
        sys.exit(1)


if __name__ == "__main__":
    main()
