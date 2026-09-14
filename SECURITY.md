# Security Policy

This project is supported under the Linux Foundation CRA stewardship framework,
as described at https://www.linuxfoundation.org/security. Security
vulnerabilities should be reported through the mechanisms described below,
which we will coordinate with our CRA steward. For actively exploited
vulnerabilities and severe incidents that may require CRA escalation, please
use the project’s emergency security reporting mechanisms as appropriate.

## Security Context

Open source software can be used in various contexts that may go far beyond
what it was originally designed and also secured for. Therefore, we describe
here how isar-cip-core is expected to be used in security-sensitive scenarios.

This project provides building blocks and reference configurations to create
secured systems that support secure updates. Demonstration images that can be
created from it may use publicly known keys or contain debugging features not
intended for production use. It is the responsibility of the final integrator
of this project to use appropriate keys and harden the system integration. This
includes the choice of secured Debian package versions.

In scope for vulnerability reports against this project are flaws in new or
customized packages provided here which contribute to the security of a final
system. Furthermore in scope are noteworthy documentation deficits regarding
how to achieve secure usage of these packages.

## Reporting a Vulnerability

Please DO NOT report any potential security vulnerability via a public channel
(mailing list, gitlab issue etc.). Instead, contact the CIP security team via
security@cip-project.org via email. Please provide a detailed description of
the issue, the steps to reproduce it, the affected versions and, if already
available, a proposal for a fix. You should receive a response within 5 working
days. If the issue is confirmed as a vulnerability by us, we will request a
CVE, publish the fix and give credits for your report if desired. This project
follows a 90 day disclosure timeline.
