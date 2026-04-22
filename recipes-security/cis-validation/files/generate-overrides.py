#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2026 Siemens AG
# SPDX-License-Identifier: MIT
#
# Generate override files for disabled CIS hardening rules
#
# Usage:
#   python3 generate_overrides.py --config-json .config.json --output overrides
#

import argparse
import json
import os
import logging

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

class OverrideGenerator:
    def __init__(self, config_json_path, output):
        self.config_json_path = config_json_path
        self.output = output
        self.rules_data = []

    def load_config_json(self):
        try:
            with open(self.config_json_path, 'r') as f:
                self.rules_data = json.load(f).get('rules', [])
        except FileNotFoundError:
            raise FileNotFoundError(
                f"Config file not found: '{self.config_json_path}'"
            )
        except json.JSONDecodeError as e:
            raise ValueError(
                f"Invalid JSON in config file '{self.config_json_path}': {e}"
            )
        logger.info(f"Successfully loaded configuration from '{self.config_json_path}'")

    def generate_files(self):
        if not self.rules_data:
            logger.warning("No rules data loaded. Skipping file generation.")
            return

        errors_during_generation = []
        generated_count = 0

        for entry in self.rules_data:
            rule = entry.get('rule', {})
            if not rule.get('apply', True):
                name = rule.get('name')
                description = rule.get('description')
                if name is None or description is None:
                    raise ValueError(
                        f"Rule entry missing required field(s): "
                        f"name={name!r}, description={description!r}"
                    )
                description = description.replace(' ', '_')
                justification = rule.get('justification', f"Rule '{name}' disabled by user configuration.")

                os.makedirs(self.output, exist_ok=True)
                logger.info(f"Ensured output directory exists: '{self.output}'")
                # Sanitize path components so a description/name cannot escape output dir.
                safe_name = os.path.basename(str(name))
                safe_desc = os.path.basename(description)
                filepath = os.path.join(self.output, f"{safe_name}_{safe_desc}.cfg")
                content = f"# {justification}\nstatus=disabled\n"

                try:
                    with open(filepath, 'w') as f:
                        f.write(content)
                    logger.info(f"Generated '{os.path.basename(filepath)}' for disabled rule '{name}'")
                    generated_count += 1
                except IOError as e:
                    errors_during_generation.append(f"Error writing file '{filepath}' for rule '{name}': {e}")
            else:
                logger.info(f"Rule '{rule.get('name')}' is applied, skipping override file generation.")

        if errors_during_generation:
            for err in errors_during_generation:
                logger.error(err)
            raise Exception(f"Completed generation with {len(errors_during_generation)} errors. Generated {generated_count} files.")
        else:
            logger.info(f"Generated {generated_count} override files in '{self.output}'")

def main():
    parser = argparse.ArgumentParser(
        description="Generate override files for disabled CIS hardening rules from .config.json"
    )
    parser.add_argument("--config-json", help="Path to .config.json", default=".config.json")
    parser.add_argument("--output", help="Directory for override files", default="overrides")
    args = parser.parse_args()

    generator = OverrideGenerator(args.config_json, args.output)
    generator.load_config_json()
    generator.generate_files()

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        logger.error(e)
        raise
