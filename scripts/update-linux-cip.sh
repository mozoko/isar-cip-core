#!/bin/bash
#
# SPDX-License-Identifier: MIT
#

# Setup GPG keys before processing
setup_gpg_keys() {
    local GPG_KEY_EMAIL="autosigner@kernel.org"

    # Check if GPG is installed
    if ! command -v gpg &> /dev/null; then
        echo "Error: GPG is not installed."
        echo "GPG is required to verify the authenticity of kernel sources."
        echo ""
        echo "Please install GPG and run the script again:"
        echo "  sudo apt-get update"
        echo "  sudo apt-get install gnupg"
        exit 1
    fi

    # Check if key is already imported
    if ! gpg --list-keys "${GPG_KEY_EMAIL}" &>/dev/null; then
        echo "Importing GPG key for ${GPG_KEY_EMAIL}..."
        gpg --locate-keys "${GPG_KEY_EMAIL}" &>/dev/null
    fi
}
# Setup GPG keys
setup_gpg_keys

REPO_ROOT=$(git rev-parse --show-toplevel)
pushd "${REPO_ROOT}"/recipes-kernel/linux >/dev/null || exit 1

for MAJOR_VERSION in 4.4 4.19 5.10 6.1 6.12; do
	rm -f sha256sums.asc
	wget -q https://mirrors.kernel.org/pub/linux/kernel/projects/cip/${MAJOR_VERSION}/sha256sums.asc
	if ! gpg --verify sha256sums.asc 2>/dev/null; then
		echo "sha256sums.asc for ${MAJOR_VERSION} invalid!"
		exit 1
	fi

	LAST_ENTRY=$(grep "cip[0-9]\+\.tar\.xz" sha256sums.asc | sort -t p -k 3n | tail -1)
	VERSION=${LAST_ENTRY/*linux-cip-/}
	VERSION=${VERSION/.tar.xz/}
	RECIPE_FILE=linux-cip_${VERSION}.bb
	if [ ! -f "${RECIPE_FILE}" ]; then
		echo "Updating recipe to ${VERSION}"
		git mv linux-cip_"${MAJOR_VERSION}".*.bb "${RECIPE_FILE}"
		SHASUM=${LAST_ENTRY/ */}
		sed -i 's/\(SRC_URI\[sha256sum\] = "\).*/\1'"${SHASUM}"'"'/ "${RECIPE_FILE}"
		git add "${RECIPE_FILE}"
		if [ "${MAJOR_VERSION}" == "6.12" ]; then
			git rm linux-cip-rt_"${MAJOR_VERSION}".*.bb
			ln -s "${RECIPE_FILE}" "linux-cip-rt_${VERSION}.bb"
			git add "linux-cip-rt_${VERSION}.bb"
		fi
	fi

	if [ "${MAJOR_VERSION}" != "6.12" ]; then
		LAST_ENTRY=$(grep "rt[0-9]\+\.tar\.xz" sha256sums.asc | sort -t p -k 3n | tail -1)
		VERSION=${LAST_ENTRY/*linux-cip-/}
		VERSION=${VERSION/.tar.xz/}
		RECIPE_FILE=linux-cip-rt_${VERSION}.bb
		if [ ! -f "${RECIPE_FILE}" ]; then
			echo "Updating recipe to ${VERSION}"
			git mv linux-cip-rt_"${MAJOR_VERSION}".*.bb "${RECIPE_FILE}"
			SHASUM=${LAST_ENTRY/ */}
			sed -i 's/\(SRC_URI\[sha256sum\] = "\).*/\1'"${SHASUM}"'"'/ "${RECIPE_FILE}"
			git add "${RECIPE_FILE}"
		fi
	fi
done

rm -f sha256sums.asc
popd >/dev/null || exit 1
