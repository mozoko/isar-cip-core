#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 Siemens AG
# SPDX-License-Identifier: MIT
#

import argparse
from pathlib import Path
import re

SECTIONS = {
    "1 Initial Setup": {
        "1.1 Filesystem Configuration": {},
        "1.2 Software Updates": {},
        "1.3 Filesystem Integrity Checking": {},
        "1.4 Secure Boot Settings": {},
        "1.5 Additional Process Hardening": {},
        "1.6 Mandatory Access Control": {},
        "1.7 CLI Warning Banners": {},
        "1.8 Gnome Display Manager": {},
    },
    "2 Services": {
        "2.1 Time Synchronization": {},
        "2.2 Special Purpose Services": {},
        "2.3 Service Clients": {},
    },
    "3 Network Configuration": {
        "3.1 Protocols and Devices": {},
        "3.2 Parameters (Host Only)": {},
        "3.3 Parameters (Host and Router)": {},
        "3.5 Firewall": {},
    },
    "4 Logging and Auditing": {
        "4.1 System Accounting (auditd)": {},
        "4.2 Logging": {},
        "4.4 Ensure logrotate assigns appropriate permissions": {},
    },
    "5 Access and Auth": {
        "5.1 Time-based Job Schedulers": {},
        "5.2 SSH Server": {},
        "5.3 Privilege Escalation": {},
        "5.4 PAM": {},
        "5.5 User Accounts and Environment": {},
        "5.6 Restrict Access to su": {}
    },
    "6 System Maintenance": {
        "6.1 System File Permissions": {},
        "6.2 Local User and Group Settings": {},
    },
}

# Maps ID of a rule and the additional Kconfig dependencies that it requires
SPECIAL_RULES = {
    "1.1.6": "CIP_VAR_PART",
    "1.1.6.1": "CIP_VAR_PART",
    "1.1.6.2": "CIP_VAR_PART",
    "1.1.7": "CIP_VAR_TMP_PART",
    "1.1.8": "CIP_VAR_TMP_PART",
    "1.1.9": "CIP_VAR_TMP_PART",
    "1.1.10": "CIP_VAR_TMP_PART",
    "1.1.11": "CIP_VAR_LOG_PART",
    "1.1.11.1": "CIP_VAR_LOG_PART",
    "1.1.11.2": "CIP_VAR_LOG_PART",
    "1.1.11.3": "CIP_VAR_LOG_PART",
    "1.1.12": "CIP_VAR_LOG_AUDIT_PART",
    "1.1.12.1": "CIP_VAR_LOG_AUDIT_PART",
    "1.1.12.2": "CIP_VAR_LOG_AUDIT_PART",
    "1.1.12.3": "CIP_VAR_LOG_AUDIT_PART",
    "1.1.13": "CIP_HOME_PART",
    "1.1.14": "CIP_HOME_PART",
    "1.1.14.1": "CIP_HOME_PART",
    "1.5.1": "CIP_GRUB_BOOTLOADER",
    "5.1.2": "CIP_CRON",
    "5.1.3": "CIP_CRON",
    "5.1.4": "CIP_CRON",
    "5.1.5": "CIP_CRON",
    "5.1.6": "CIP_CRON",
    "5.1.7": "CIP_CRON",
    "5.1.8": "CIP_CRON",
}

SPECIAL_RULE_JUSTIFICATIONS = {
    "CIP_VAR_PART": "Not applicable: no separate /var partition is configured.",
    "CIP_VAR_TMP_PART": "Not applicable: no separate /var/tmp partition is configured.",
    "CIP_VAR_LOG_PART": "Not applicable: no separate /var/log partition is configured.",
    "CIP_VAR_LOG_AUDIT_PART": "Not applicable: no separate /var/log/audit partition is configured.",
    "CIP_HOME_PART": "Not applicable: no separate /home partition is configured.",
    "CIP_GRUB_BOOTLOADER": "Not applicable: the GRUB bootloader is not used.",
    "CIP_CRON": "Not applicable: cron is not used on this system.",
}


class Rule:
    def __init__(self, rule: Path):
        if "_" not in rule.name:
            raise ValueError(
                f"Rule filename '{rule.name}' does not match expected '<id>_<name>' format"
            )
        id, name = rule.name.split("_", 1)
        self.id = [int(i) for i in id.split(".")]
        self.name = name.split(".")[0].replace("_", " ")
        self.description = None
        self.hardening_level = 5
        with open(rule, "r") as f:
            for line in f:
                line = line.strip()
                if line.startswith("DESCRIPTION="):
                    self.description = line.split("=", 1)[1].strip('"')
                elif line.startswith("HARDENING_LEVEL="):
                    raw = line.split("=", 1)[1].strip()
                    try:
                        self.hardening_level = int(raw)
                    except ValueError:
                        raise ValueError(
                            f"Rule '{rule.name}' has non-integer HARDENING_LEVEL={raw!r}"
                        )


def setup_parser():
    parser = argparse.ArgumentParser(
        prog="cis-hardening-to-kconfig",
        description="Convert the OVH CIS hardening rules to a KConfig",
    )
    parser.add_argument("rules-dir", type=Path)
    return parser


def rule_to_kconfig_entry(r: Rule, parent_symbol: str, levels: list[int]) -> str:
    id_str = "_".join(str(i) for i in r.id)
    id_dotted = ".".join(str(i) for i in r.id)
    symbol = f"CIS_{id_str}"
    applicable = [l for l in levels if l >= r.hardening_level]
    default_cond = " || ".join(f"CIS_LEVEL_{l}" for l in applicable)
    additional_cond = SPECIAL_RULES.get(id_dotted)
    if additional_cond:
        default_cond = f"({default_cond}) && {additional_cond}"
    lines = [
        f"config {symbol}",
        f'\tbool "{id_dotted} - {r.name} ({r.hardening_level})"',
        f"\tdefault y if {default_cond}",
    ]
    if r.description:
        lines.append(f"\thelp")
        lines.append(f"\t  {r.description}")
    return "\n".join(lines) + "\n"


def transform_prompt(bool_prompt):
    """
    Turn the bool prompt into a justification string prompt.
    e.g. "1.1.1.1 - disable freevxfs (2)"
      -> "justification to enable freevxfs (2)"
    We strip the leading number and dash, remove leading verb, keep the rest.
    """
    # Remove the CIS rule number prefix  "X.X.X - "
    m = re.match(r"^[\d.]+ - (.+)$", bool_prompt)
    if m:
        rest = m.group(1)
    else:
        rest = bool_prompt

    # Drop leading verb ("disable", "enable", "install", "restrict", etc.)
    rest_stripped = re.sub(
        r"^(disable|enable|install|restrict|configure|use|set|remove|find|check|limit|lock|freeze|enforce|log|record|halt|keep|make|update)\s+",
        "",
        rest,
        flags=re.IGNORECASE,
    )

    return f"justification to enable {rest_stripped}"


def transform_help(help_text):
    """
    Build a generic justification help line from the parent help text.
    Strip leading verb (same list as transform_prompt) before composing.
    """
    # Strip trailing period
    stripped = help_text.rstrip(".")
    # Drop leading verb
    stripped = re.sub(
        r"^(disable|enable|install|restrict|configure|use|set|remove|find|check|"
        r"limit|lock|freeze|enforce|log|record|halt|keep|make|update|collect|"
        r"ensure|verify|deactivate|activate|implement|implemet|create|"
        r"checking|allow|disallow|do not allow)\s+",
        "",
        stripped,
        flags=re.IGNORECASE,
    )
    return f"Provide a reason for enabling {stripped} for the current hardening level."


def rule_to_justification_entry(r: Rule, parent_symbol: str, levels: list[int]) -> str:
    id_str = "_".join(str(i) for i in r.id)
    id_dotted = ".".join(str(i) for i in r.id)
    symbol = f"CIS_{id_str}_JUSTIFICATION"
    applicable = [l for l in levels if l >= r.hardening_level]
    level_cond = " || ".join(f"CIS_LEVEL_{l}" for l in applicable)
    additional_cond = SPECIAL_RULES.get(id_dotted)
    depends_cond = f"!CIS_{id_str} && ({level_cond})"
    prompt = transform_prompt(f"{id_dotted} - {r.name} ({r.hardening_level})")
    lines = [
        f"config {symbol}",
        f'\tstring "{prompt}"',
        f"\tdepends on {depends_cond}",
    ]
    if additional_cond:
        preset = SPECIAL_RULE_JUSTIFICATIONS.get(additional_cond)
        if preset:
            lines.append(f'\tdefault "{preset}" if !{additional_cond}')
    help_text = transform_help(r.description) if r.description else None
    if r.description:
        lines.append("\thelp")
        lines.append(f"\t  {help_text}")
    return "\n".join(lines) + "\n"


def section_symbol(section_key: str) -> str:
    num = section_key.split(" ", 1)[0]
    return "CIS_SEC_" + num.replace(".", "_")


def generate_kconfig(rules: list[Rule]) -> str:
    # Collect all hardening levels used
    levels = sorted({r.hardening_level for r in rules if r.hardening_level is not None})
    if not levels:
        raise ValueError("No rules with a hardening level found; cannot generate Kconfig")

    # Build lookup: subsection number -> list of rules
    subsection_rules: dict[str, list[Rule]] = {}
    for rule in rules:
        key = ".".join(str(i) for i in rule.id[:2])
        subsection_rules.setdefault(key, []).append(rule)

    output = []

    # Hardening level selection
    output.append('menu "Default hardening level"')
    output.append("")
    output.append("choice")
    output.append('\tprompt "Hardening level"')
    default_level = 2 if 2 in levels else levels[0]
    output.append(f"\tdefault CIS_LEVEL_{default_level}")
    output.append("")

    for level in levels:
        output.append(f"config CIS_LEVEL_{level}")
        output.append(f'\tbool "Level {level}"')
        output.append("")

    output.append("endchoice")
    output.append("")
    output.append("endmenu")
    output.append("")

    # Other special rule menus
    output.append('menu "Partitioning layout"')
    output.append("")
    output.append("config CIP_HOME_PART")
    output.append('\tbool "Separate /home partition"')
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you have a separate /home partition")
    output.append("")
    output.append("config CIP_VAR_PART")
    output.append('\tbool "Separate /var partition"')
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you have a separate /var partition")
    output.append("")
    output.append("config CIP_VAR_TMP_PART")
    output.append('\tbool "Separate /var/tmp partition"')
    output.append("\tdepends on CIP_VAR_PART")
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you have a separate /var/tmp partition")
    output.append("")
    output.append("config CIP_VAR_LOG_PART")
    output.append('\tbool "Separate /var/log partition"')
    output.append("\tdepends on CIP_VAR_PART")
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you have a separate /var/log partition")
    output.append("")
    output.append("config CIP_VAR_LOG_AUDIT_PART")
    output.append('\tbool "Separate /var/log/audit partition"')
    output.append("\tdepends on CIP_VAR_PART && CIP_VAR_LOG_PART")
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you have a separate /var/log/audit partition")
    output.append("")
    output.append("endmenu")
    output.append("")
    output.append('menu "Additional options"')
    output.append("config CIP_GRUB_BOOTLOADER")
    output.append('\tbool "GRUB Bootloader"')
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you use the GRUB bootloader")
    output.append("")
    output.append("config CIP_CRON")
    output.append('\tbool "CRON"')
    output.append("\tdefault n")
    output.append("\thelp")
    output.append("\t  Enable this if you use CRON")
    output.append("")
    output.append("endmenu")
    output.append("")

    for section_key, subsections in SECTIONS.items():
        output.append(f'menu "{section_key}"')
        output.append("")

        for subsection_key in subsections:
            sub_num = subsection_key.split(" ", 1)[0]
            matching = subsection_rules.get(sub_num, [])
            if not matching:
                continue

            output.append(f'menu "{subsection_key}"')
            output.append("")

            for rule in matching:
                output.append(rule_to_kconfig_entry(rule, None, levels))
                output.append(rule_to_justification_entry(rule, None, levels))

            output.append("endmenu")
            output.append("")

        output.append("endmenu")
        output.append("")

    return "\n".join(output)


def main():
    parser = setup_parser()
    args = parser.parse_args()
    rulesdir: Path = getattr(args, "rules-dir")

    rules = sorted(
        (Rule(f) for f in rulesdir.iterdir() if f.is_file() and "_" in f.name),
        key=lambda r: r.id,
    )

    print(generate_kconfig(rules))


if __name__ == "__main__":
    main()
