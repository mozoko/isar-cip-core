# CIS Hardening

CIP Core can generate and apply a selected set of CIS hardening rules during
the image build.  The rule catalog is derived from OVH `debian-cis` and is
exposed as a Kconfig menu.  The selected rules are exported as JSON, converted
into Debian package content, and reused for kernel configuration and optional
runtime validation.

## Key design decisions

- CIS hardening is optional.
- Hardening is enabled by a single image configuration option,
  `CIP_CIS_HARDENING`, plus the selected rule configuration.
- Hardening and validation are independent.  An image may apply hardening,
  but skip the validation support. Only validating is not supported, though.
- Hardening and validation configuration must be overridable from a
  third-party layer, for example through a `cip-cis-rules-config_%.bbappend` that
  provides a checked-in configuration.
- The default level `2` configuration must harden all generic settings.
  A conformity level below `100%` is acceptable for this default because the
  remaining rules are product specific and must be completed downstream.

## Enabling CIS hardening

Enable hardening for an image by setting `CIP_CIS_HARDENING`:

```
CIP_CIS_HARDENING = "1"
```

When this option is enabled, `cip-core-image`:

- inherits the CIS hardening image class
- installs the `cip-cis-rules-config-${MACHINE}` package
- applies the generated hardening package to the root file system,
- adds the generated kernel hardening fragment to the CIP kernel build.

## Configuring rules

Rules can be selected with the recipe menuconfig task:

```
./kas-container shell <kas files>
bitbake cip-cis-rules-config -c menuconfig
bitbake cip-core-image
```

The menu contains:

- a hardening level choice, currently levels `1` to `5`,
- one boolean option per CIS rule,
- justification strings for rules disabled even though the selected level
  requires them.

Changing the hardening level in the menu resets rule selections to the defaults
for the new level.  Existing justification strings are kept.

The menu writes the configuration to `${S}/${HARDENING_CFG_NAME}` in the work
directory of `cip-cis-rules-config` (`config.json` if `HARDENING_CFG` is not set).
After the package build it is deployed to
`${DEPLOY_DIR_IMAGE}/${CIP_CIS_HARDENING_CONFIG}`.

No default configuration is shipped: a build with `CIP_CIS_HARDENING` enabled
fails in `do_check_config` until a configuration is provided.

For product builds, copy the generated config into a local layer and select it
with `HARDENING_CFG` from a `cip-cis-rules-config_%.bbappend`, e.g.:

```
FILESEXTRAPATHS:prepend := "${FILE_DIRNAME}/files:"
HARDENING_CFG = "example.config.json"
```

The file named in `HARDENING_CFG` is added to `SRC_URI` automatically.  Check
both the bbappend and the config into version control.  You can have multiple
configurations checked in this way, and select which one should be applied with
`HARDENING_CFG`.

The JSON format is validated against
`recipes-security/cip-cis-rules-config/files/scripts/config.schema.json`,
whereby `apply` controls whether the rule is enabled.  `justification` documents
why a required rule is disabled.

## What the hardening package installs

The `cip-cis-rules-config` package is a configuration package.  It installs only the
files for rules with `apply: true`; rules without implemented snippets are
kept in the JSON selection but do not add files to the root file system.

Implemented snippets currently cover:

- `/etc/modprobe.d/` files for disabled filesystems and USB storage,
- `/etc/sysctl.d/` files for kernel and network sysctl settings,
- `/etc/ssh/sshd_config.d/` files for SSH daemon hardening,
- `/etc/sudoers.d/` files for sudo hardening,
- `/etc/security/limits.d/` and `/etc/security/pwquality.d/`,
- PAM snippets under `/usr/share/pam-configs` and `/etc/pam.d`,
- chrony configuration under `/etc/chrony/conf.d`,
- helper scripts under `/usr/share/cip-cis-rules/configuration` for settings
  that require an install-time action, such as file permissions,
  `pam-auth-update`, and `/etc/login.defs` edits.

The `cip-cis-rules-config` package installs the generated data artifacts under
`/usr/share/cip-cis-rules/`:

- `config.json`, the selected rule set,
- `hardening.cfg`, the kernel config fragment generated from enabled
  kernel-relevant rules.

The same artifacts are also deployed to `DEPLOY_DIR_IMAGE` as:

- `${CIP_CIS_HARDENING_CONFIG}`,
- `${CIP_CIS_HARDENING_KCONFIG}`.

## Kernel hardening

When `CIP_CIS_HARDENING` is enabled, the CIP kernel includes
`${DISTRO}-hardening.kernel.cfg` as `hardening.kernel.cfg` through
`KERNEL_CONFIG_FRAGMENTS`.

The fragment is generated from enabled rules only.  Disabled rules are omitted
from the kernel fragment and should carry a justification in `config.json` if
they are required by the selected level.

## Validation

Set `CIP_CIS_HARDENING_VALIDATION` to enable validation support in the image:

```
CIP_CIS_HARDENING_VALIDATION = "1"
```

With validation enabled, `cip-core-image` installs `cis-validation`, and the CIP
kernel build additionally checks the generated hardening fragment against the
kernel configuration it produced.  Validation requires `CIP_CIS_HARDENING` to
provide the selected CIS rule configuration and generated kernel hardening
fragment.

There are two independent validation stages: a **build-time** check of the
generated kernel configuration, and a **runtime** audit of the target.  Both use
the same selected CIS rule configuration, so they verify that the policy you
picked is the policy that is actually applied.

### What is validated

- **Build-time kernel validation** verifies that the CIS hardening fragment
  actually took effect in the kernel configuration produced by the CIP kernel
  build.
- **Runtime validation** runs the OVH `cis-hardening` audit on the target and
  requires full conformity with the selected policy.

### Build-time kernel validation

The generated hardening kernel fragment is checked by the
`do_validate_kernel_config` task from
`classes-recipe/cis-kernel-hardening-validation.bbclass`.  The CIP kernel
recipe inherits that class only when both `CIP_CIS_HARDENING` and
`CIP_CIS_HARDENING_VALIDATION` are set; the check is gated by the `inherit`
itself, not by a task flag.

The task runs `after do_dpkg_build before do_deploy_deb`, so a failure stops
the kernel before its package reaches `isar-apt`.

It compares the fragment against the `/boot/config-*` file shipped inside the
generated `linux-image-*` package.  That package is the only place the merged
configuration exists: Isar compiles the kernel with sbuild inside an ephemeral
chroot, so `merge_config.sh` writes the merged `.config` there, and what
remains under `${S}` is the pre-merge staged defconfig without the fragment
applied.

Both directions of the fragment are checked:

- options that a CIS rule requires to be disabled must be absent from the merged
  configuration — `=m` is a violation as well as `=y`, since a disabled
  filesystem must not be buildable at all;
- options that a rule requires to be enabled must be `=y`.

The task writes a JSON compliance report to
`${DEPLOY_DIR_IMAGE}/${CIP_CIS_HARDENING_REPORT}`, by default
`${DISTRO}-hardening-kernel-${KERNEL_NAME_PROVIDED}-report.json`.  Every checked rule is listed with a
`compliant` flag and a per-symbol breakdown, where `actual` is the literal
string `not set` when a symbol is absent from the kernel configuration:

```json
{
  "rules": [
    {
      "rule": {
        "name": "1.1.1.5",
        "description": "Disable SquashFS filesystem",
        "compliant": true,
        "symbols": [
          {
            "symbol": "CONFIG_SQUASHFS",
            "expected": "n",
            "actual": "not set",
            "compliant": true
          }
        ]
      }
    }
  ]
}
```

If any expectation is unmet, the kernel build fails and names every offending
rule, symbol, expected value and actual value.  In that case the report is not
deployed; it is left in the recipe's `deploy-hardening` directory, and the
fatal message names that path.

Validation applies only to kernels built from this layer, because only those
receive the hardening fragment.  An image that uses an upstream or Debian kernel
gets no build-time kernel check; for such kernels the relevant CIS rules are
enforced by the `/etc/modprobe.d` drop-ins installed by `cip-cis-rules-config`
and verified by the runtime audit.

### Runtime CIS audit

On the target, the `cis-validation` service runs the OVH `cis-hardening`
audit (`hardening.sh --audit`).  The audit checks the *selected* policy rather
than the complete upstream rule set:

- Rules disabled in `${CIP_CIS_HARDENING_CONFIG}` are converted into override
  files with `status=disabled` during the image build
  (`generate-overrides.py`) so the audit skips them.
- SSH overrides from the hardening package are mapped to their real audit
  targets at package install time (`postinst`).
- Non-CIS rules (`99.*`) are dropped from the audit (as they highly
  opinionated and not part of the CIS hardening guide).

The audit runs in a temporary CIS configuration tree on a tmpfs mount, with a
bind-mounted `/etc/default/cis-hardening`, so it never modifies the running
(and possibly read-only) file system.  It succeeds only when the reported
`CONFORMITY_PERCENTAGE` is `100.00`; otherwise it exits non-zero.

- **When:** during boot up the `cis-validation.service` is started
  after `network-online` is up.
  It can also be run manually with `/usr/bin/cis-validation`.
- **Why:** it detects configuration drift over the lifetime of the device, so
  the target remains demonstrably conformant long after it left the factory,
  not just at build time.

## Notes and limitations

Some CIS rules are inherently use case specific. Examples are partition layout
rules, firewall policy, SSH access lists, password ageing, and rules that
inspect existing users or files.  Configure or justify those rules according
to the target product requirements before relying on validation results.

The hardening package will **not** install additional packages to fulfill
a rule, for example iptables or ssh. These use case specific packages need to
be installed by the user.

### Comparison to `security-customizations`

The recipe `security-customizations` sets some hardening options similar to the recipe `cip-cis-rules`.
Because `security-customizations` is part of the certification efforts, it is not modified. The following sections
list the touch points.

#### SSH hardening

The recipe `security-customizations` contains hardening rules for ssh. This table summarizes the differences from
the cip-cis-rules:
| security-customizations | cis-rules |Comment|
| ------ | ------ |------|
|ssh-remote-session-term.conf|cis-5.2.16-sshd-idle-timeout.conf| adapt cis hardening to lower values of cip security|
|ssh-pam-remote.conf        |    -    | no rule implemented in cip-cis-rules |
|ssh-hardening.conf        |cis-5.2.7-sshd-maxauthtries.conf        | 3 vs 4 |
|ssh-hardening.conf        |cis-5.2.23-limit-ssh-max-sessions.conf        |2 vs 10|
|ssh-hardening.conf        |cis-5.2.21-disable-ssh-allow-tcp-forwarding.conf        ||
|ssh-hardening.conf        |cis-5.2.6-disable-x11-forwarding.conf        ||
|ssh-hardening.conf        | -       | AllowAgentForwarding is allowed according to cis rules|

####  Other hardening

The script `postinst` of the recipe `security-customizations` contains the following changes:
|security-customizations|cis-rules|Comment|
| ------ | ------ |------|
| set password creation parameters with pam.cracklib| | parameter differ between the implementations|
|lock user account after unsuccessful attempts| - | no rule implemented in cip-cis-rules|
| Concurrent session control| - |no rule implemented in cip-cis-rules|
| Audit storage capacity| - |no rule implemented in cip-cis-rules|
| Enable Multi Factor Authentication for Local and Remote Session| - |no rule implemented in cip-cis-rules|
