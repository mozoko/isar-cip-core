#!/usr/bin/env python3
#
# Based on the kas menu plugin
# Copyright (c) Siemens AG, 2021-2023
# Copyright (c) 2011-2019, Ulf Magnusson <ulfalizer@gmail.com>
# SPDX-License-Identifier: MIT
#

import argparse
import json
import logging
import pathlib
import re
import sys
import traceback

from kconfiglib import (
    Kconfig,
    Symbol,
    Choice,
    expr_value,
    TYPE_TO_STR,
    MENU,
    COMMENT,
    STRING,
    BOOL,
    INT,
    HEX,
    UNKNOWN,
)

from snack import (
    SnackScreen,
    EntryWindow,
    ButtonChoiceWindow,
    ButtonBar,
    Listbox,
    GridFormHelp,
)

logger = logging.getLogger(__name__)


class Menuconfig:
    def __init__(self, kconf):
        self.kconf = kconf
        self.screen = None

    @staticmethod
    def value_str(sym):
        if sym.type in (STRING, INT, HEX):
            return f"({sym.str_value})"

        if sym.choice and sym.visibility == 2:
            return "(*)" if sym.choice.selection is sym else "( )"

        tri_val_str = (" ", None, "*")[sym.tri_value]

        if len(sym.assignable) == 1:
            return f"-{tri_val_str}-"

        if sym.type == BOOL:
            return f"[{tri_val_str}]"

        raise RuntimeError()

    @staticmethod
    def node_str(node, indent):
        if not node.prompt:
            return ""

        prompt, prompt_cond = node.prompt
        if not expr_value(prompt_cond):
            return ""

        if node.item == MENU:
            return f"    {indent * ' '}{prompt}  --->"

        if type(node.item) is Choice:
            return f"    {indent * ' '}{prompt}"

        if node.item == COMMENT:
            return f"    {indent * ' '}*** {prompt} ***"

        sym = node.item

        if sym.type == UNKNOWN:
            return ""

        res = f"{Menuconfig.value_str(sym):3} {indent * ' '}{prompt}"

        if node.is_menuconfig:
            res += f"  ---{'>' if sym.tri_value > 0 else '-'}"

        return res

    @staticmethod
    def menu_node_strings(node, indent):
        items = []

        while node:
            string = Menuconfig.node_str(node, indent)
            if string:
                items.append((string, node))

            if (
                node.list
                and node.item != MENU
                and (type(node.item) is Choice or not node.is_menuconfig)
            ):
                items.extend(Menuconfig.menu_node_strings(node.list, indent + 2))

            node = node.next

        return items

    def show_menu(self, title, top_node, is_submenu=False):
        selection = 0

        while True:
            items = Menuconfig.menu_node_strings(top_node, 0)

            height = len(items)
            window_width = 60

            scroll = 0
            if height > self.screen.height - 13:
                height = self.screen.height - 13
                scroll = 1

            buttons = [
                ("Save & Exit", "save", "S"),
                (" Exit ", "exit", "E"),
                (" Help ", "help", "h"),
            ]
            if is_submenu:
                buttons.insert(0, (" Return ", "return", "ESC"))
            buttonbar = ButtonBar(self.screen, buttons)
            if not is_submenu:
                buttonbar.hotkeys["ESC"] = "exit"
            listbox = Listbox(height, width=window_width, scroll=scroll, returnExit=1)
            count = 0
            for string, _ in items:
                listbox.append(string, count)
                if selection == count:
                    listbox.setCurrent(count)
                count += 1

            grid = GridFormHelp(self.screen, title, None, 1, 2)
            grid.add(listbox, 0, 0, padding=(0, 0, 0, 1))
            grid.add(buttonbar, 0, 1, growx=1)
            grid.addHotKey(" ")

            # Snapshot the active CIS level so we can detect level changes
            # caused by the user action below and re-derive rule defaults.
            level_before = _selected_level(self.kconf)

            rc = grid.runOnce()

            action = buttonbar.buttonPressed(rc)
            if action and action != "help":
                return action

            if count == 0:
                continue

            selection = listbox.current()
            _, selected_node = items[selection]
            sym = selected_node.item

            if action == "help":
                prompt, _ = selected_node.prompt
                if selected_node.help:
                    help_text = selected_node.help
                else:
                    help_text = "No help available."
                ButtonChoiceWindow(
                    screen=self.screen,
                    title=f"Help on '{prompt}'",
                    text=help_text,
                    width=window_width,
                    buttons=["  Ok  "],
                )
                continue

            show_submenu = False

            if type(sym) is Symbol:
                if rc == " ":
                    if sym.type == BOOL:
                        sym.set_value("n" if sym.tri_value > 0 else "y")
                else:
                    if selected_node.is_menuconfig:
                        show_submenu = True
                    elif sym.type in (STRING, INT, HEX):
                        action, values = EntryWindow(
                            screen=self.screen,
                            title=sym.name,
                            text=f"Enter a {TYPE_TO_STR[sym.type]} value:",
                            prompts=[("", sym.str_value)],
                            width=80,
                            entryWidth=80,
                            buttons=[("  Ok  ", "Ok"), ("Cancel", "", "ESC")],
                        )
                        if action == "Ok":
                            self.kconf.warnings = []
                            val = values[0]
                            if sym.type == HEX and not val.startswith("0x"):
                                val = "0x" + val
                            sym.set_value(val)
                            sym.str_value
                            if len(self.kconf.warnings) > 0:
                                ButtonChoiceWindow(
                                    screen=self.screen,
                                    title="Invalid entry",
                                    text="\n".join(self.kconf.warnings),
                                    width=window_width,
                                    buttons=["  Ok  "],
                                )
                                self.kconf.warnings = []
            elif selected_node.is_menuconfig and type(sym) is not Choice:
                show_submenu = True

            if show_submenu:
                submenu_title, _ = selected_node.prompt
                action = self.show_menu(
                    submenu_title, selected_node.list, is_submenu=True
                )
                if action != "return":
                    return action

            # If the user just switched CIS_LEVEL_N, drop any user overrides
            # on rule symbols so the new level's `default y if CIS_LEVEL_…`
            # clauses take effect and every rule required by the new level
            # is enabled by default.  Justifications and the level symbols
            # themselves are preserved.
            level_after = _selected_level(self.kconf)
            if (
                level_before is not None
                and level_after is not None
                and level_after != level_before
            ):
                cleared = _reset_rule_overrides(self.kconf)
                ButtonChoiceWindow(
                    screen=self.screen,
                    title="Hardening level changed",
                    text=(
                        f"Switched from level {level_before} to level "
                        f"{level_after}.\n\n"
                        f"All rule selections were reset to the level "
                        f"{level_after} defaults ({cleared} prior override"
                        f"{'s' if cleared != 1 else ''} discarded).\n\n"
                        "Per-rule justifications were kept."
                    ),
                    width=window_width,
                    buttons=["  Ok  "],
                )

    def show(self):
        self.screen = SnackScreen()

        action = self.show_menu(self.kconf.mainmenu_text, self.kconf.top_node.list)

        self.screen.finish()
        return action


def _collect_level_numbers(expr):
    """Return the set of integer level numbers for all CIS_LEVEL_N symbols in a kconfiglib expr."""
    from kconfiglib import Symbol as _Symbol

    levels = set()
    if isinstance(expr, _Symbol):
        m = re.match(r"^CIS_LEVEL_(\d+)$", expr.name)
        if m:
            levels.add(int(m.group(1)))
    elif isinstance(expr, tuple):
        for sub in expr[1:]:
            levels |= _collect_level_numbers(sub)
    return levels


_LEVEL_RE = re.compile(r"^CIS_LEVEL_(\d+)$")


def _selected_level(kconf):
    """Return the integer N of the currently selected CIS_LEVEL_N choice, or None."""
    for n in range(1, 6):
        sym = kconf.syms.get(f"CIS_LEVEL_{n}")
        if sym is not None and sym.tri_value == 2:
            return n
    return None


def _reset_rule_overrides(kconf):
    """Clear user-set values on every CIS_<id> rule symbol so that the
    Kconfig ``default y if CIS_LEVEL_N || …`` clauses take effect again.

    This is invoked whenever the user picks a different CIS_LEVEL_N in the
    menu so that switching level immediately enables the rules required by
    the newly-selected level (and disables any that the old level required
    but the new one does not).

    CIS_LEVEL_N symbols themselves and the *_JUSTIFICATION string symbols
    are intentionally left alone:

      * resetting the level symbols would re-trigger us recursively;
      * justifications belong to the operator, not the level, and would be
        lost on every level change.

    Site-specific string symbols (e.g. ``CIS_2_2_1_3_SERVER``,
    ``CIS_5_2_18_GROUPS``, ``CIS_5_3_3_REMEMBER``) are likewise preserved
    so the operator does not lose typed-in values when bumping levels.

    Returns the number of rule symbols whose override was cleared.
    """
    cleared = 0
    for name, sym in kconf.syms.items():
        if not name.startswith("CIS_"):
            continue
        if _LEVEL_RE.match(name):
            continue
        if name.endswith("_JUSTIFICATION"):
            continue
        # Preserve user-supplied site-specific values (e.g. NTP server,
        # SSH allowed groups, password-history depth) — these are not
        # tied to a particular hardening level.
        if sym.type == STRING:
            continue
        # ``user_value`` is None iff the symbol has no explicit user override.
        if getattr(sym, "user_value", None) is not None:
            sym.unset_value()
            cleared += 1
    return cleared


def _find_value_symbol(kconf, rule_sym_name):
    """Return the user-tunable string symbol associated with rule
    *rule_sym_name* (e.g. ``CIS_5_3_3``), or ``None`` if the rule has no
    site-specific parameter.

    The convention is that a rule ``CIS_<id>`` may have one companion
    string symbol named ``CIS_<id>_<SUFFIX>`` (e.g. ``CIS_5_3_3_REMEMBER``,
    ``CIS_2_2_1_3_SERVER``, ``CIS_5_2_18_GROUPS``) whose value is
    substituted for ``@@VALUE@@`` in the rule's snippet.  The
    ``_JUSTIFICATION`` companion symbol is excluded — it is operator
    bookkeeping, not a snippet value.
    """
    prefix = rule_sym_name + "_"
    for name, sym in kconf.syms.items():
        if (
            name.startswith(prefix)
            and not name.endswith("_JUSTIFICATION")
            and sym.type == STRING
        ):
            return sym
    return None


_SCHEMA_PATH = pathlib.Path(__file__).parent / "config.schema.json"


def _validate_json(rules):
    """Validate *rules* against config.schema.json.

    Requires the ``jsonschema`` package.  Raises ``ImportError`` when the
    package is not installed, or ``jsonschema.ValidationError`` when the
    data does not conform to the schema.
    """
    try:
        import jsonschema
    except ImportError as exc:
        raise ImportError(
            "jsonschema is required for schema validation: "
            "install it with 'pip install jsonschema'"
        ) from exc

    with open(_SCHEMA_PATH) as f:
        schema = json.load(f)

    jsonschema.validate(instance=rules, schema=schema)


def save_as_json(kconf, path):
    """Export the current Kconfig selection state to a JSON file.

    The output is an object with two keys:

    * ``level`` — the currently selected CIS hardening level (1–5)
    * ``rules`` — list of rule objects::

          {"rule": {"name": "1.1.1.1", "description": "...", "level": 2,
                    "apply": true, "justification": "..."}}

    The ``justification`` key is only present when the corresponding
    ``CIS_<ID>_JUSTIFICATION`` symbol has a non-empty string value.

    After writing, the file is validated against config.schema.json located
    in the same directory as this script.  A ``jsonschema.ValidationError``
    is raised if the output does not conform to the schema.
    """
    selected_level = _selected_level(kconf)

    rules = []
    for name, sym in sorted(kconf.syms.items()):
        if (
            not name.startswith("CIS_")
            or name.startswith("CIS_LEVEL_")
            or name.endswith("_JUSTIFICATION")
            or sym.type != BOOL
            or not sym.nodes
        ):
            continue

        node = sym.nodes[0]
        if not node.prompt:
            continue
        prompt_text = node.prompt[0]

        # Parse "1.1.1.1 - disable freevxfs (2)" -> cis_number, description
        m = re.match(r"^([\d.]+)\s*-\s*(.+?)(?:\s*\(\d+\))?$", prompt_text)
        cis_number = m.group(1).strip() if m else name
        description = m.group(2).strip() if m else prompt_text

        # Minimum level from the first `default y if` condition
        level = None
        for _val, cond in sym.defaults:
            nums = _collect_level_numbers(cond)
            if nums:
                level = min(nums)
                break

        if level is None:
            logger.warning(
                "save_as_json: could not determine CIS level for symbol '%s', skipping",
                name,
            )
            continue

        rule = {
            "name": cis_number,
            "description": description,
            "level": level,
            "apply": sym.tri_value == 2,
        }

        # justification: only when the symbol exists and has a non-empty value
        just_sym = kconf.syms.get(name + "_JUSTIFICATION")
        if just_sym and just_sym.str_value:
            rule["justification"] = just_sym.str_value

        # value: only when the rule has a companion string symbol with a
        # non-empty value — used for snippets containing @@VALUE@@.
        val_sym = _find_value_symbol(kconf, name)
        if val_sym and val_sym.str_value:
            rule["value"] = val_sym.str_value

        rules.append({"rule": rule})

    if selected_level is None:
        raise ValueError(
            "save_as_json: no CIS level selected — set one of the CIS_LEVEL_N symbols"
        )

    output = {
        "level": selected_level,
        "rules": rules,
    }

    with open(path, "w") as f:
        json.dump(output, f, indent=2)

    _validate_json(output)


def load_config_json(kconf, path):
    """Load a previously saved JSON config file and apply it to *kconf*.

    The file is validated against config.schema.json before any symbols are
    touched.  The following is applied from the file:

    * ``level`` — sets the matching ``CIS_LEVEL_N`` choice symbol to ``y``.
    * For each rule in ``rules``:

      - The corresponding ``CIS_<id>`` bool symbol is set to ``y`` or ``n``
        according to the ``apply`` field.
      - When a non-empty ``justification`` value is present, it is written to
        the matching ``CIS_<id>_JUSTIFICATION`` string symbol.
      - Unknown rule names (no matching symbol in *kconf*) are logged as
        warnings and skipped.

    Raises ``FileNotFoundError`` when *path* does not exist,
    ``jsonschema.ValidationError`` when the file fails schema validation, and
    ``json.JSONDecodeError`` when the file is not valid JSON.
    """
    with open(path) as f:
        data = json.load(f)

    # Apply selected level
    level_sym = kconf.syms.get(f"CIS_LEVEL_{data['level']}")
    if level_sym is not None:
        level_sym.set_value("y")
    else:
        raise ValueError(
            f"load_config_json: unknown CIS level '{data['level']}' in '{path}'"
        )

    # Apply rules
    for entry in data["rules"]:
        rule = entry["rule"]
        # "1.1.1.1" -> "CIS_1_1_1_1"
        sym_name = "CIS_" + rule["name"].replace(".", "_")

        sym = kconf.syms.get(sym_name)
        if sym is None:
            logger.warning("load_config_json: unknown rule '%s', skipping", sym_name)
            continue

        sym.set_value("y" if rule["apply"] else "n")

        justification = rule.get("justification")
        if justification:
            just_sym = kconf.syms.get(sym_name + "_JUSTIFICATION")
            if just_sym is not None:
                just_sym.set_value(justification)
            else:
                logger.warning(
                    "load_config_json: no JUSTIFICATION symbol for '%s', skipping",
                    sym_name,
                )

        value = rule.get("value")
        if value:
            val_sym = _find_value_symbol(kconf, sym_name)
            if val_sym is not None:
                val_sym.set_value(value)
            else:
                logger.warning(
                    "load_config_json: no value symbol for '%s', skipping",
                    sym_name,
                )

    logger.info(
        "Loaded level %d and %d rules from %s", data["level"], len(data["rules"]), path
    )


def setup_parser():
    parser = argparse.ArgumentParser(
        prog="select-hardenings",
        description="Interactive menu for selecting CIS hardening rules",
    )
    parser.add_argument("kconfig", help="Kconfig file", nargs="?", default="Kconfig")
    parser.add_argument(
        "--output", help="Output .config.json file", default=".config.json"
    )
    parser.add_argument("--level", help="set the hardening level", default="2")
    parser.add_argument(
        "--non-interactive",
        help="skip the menu-config and generate a configuration for the given level",
        action="store_true",
    )
    return parser


def main():
    parser = setup_parser()
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(levelname)s: %(message)s",
    )

    kconf = Kconfig(args.kconfig, warn=False)
    json_path = args.output
    try:
        load_config_json(kconf, json_path)
    except FileNotFoundError:
        logger.warning("no configuration found")

    save_config = False
    if not args.non_interactive:
        menu = Menuconfig(kconf)
        action = menu.show()
        if action != "exit":
            save_config = True
    else:
        save_config = True
        try:
            requested_level = int(args.level)
        except (TypeError, ValueError):
            logger.error("Invalid hardening level '%s'. Valid levels: 1-5.", args.level)
            sys.exit(1)
        if _selected_level(kconf) != requested_level:
            level_sym = kconf.syms.get(f"CIS_LEVEL_{requested_level}")
            if level_sym is None:
                logger.error("Unknown hardening level '%s'. Valid levels: 1-5.", args.level)
                sys.exit(1)
            level_sym.set_value("y")

    if save_config:
        kconf.write_config(args.output)
        try:
            save_as_json(kconf, json_path)
            logger.info("Hardening rules written to: %s", json_path)
        except Exception:
            error_path = json_path + ".error"
            with open(error_path, "w") as f:
                traceback.print_exc(file=f)
            logger.error("save_as_json failed — details in %s", error_path)
            sys.exit(1)


if __name__ == "__main__":
    main()
