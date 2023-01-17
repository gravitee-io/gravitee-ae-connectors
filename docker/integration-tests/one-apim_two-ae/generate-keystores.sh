#!/usr/bin/env bash
#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -euo pipefail

generate_keystores_and_certificate() {
  cat <<EOF
##################################
###  Generate all certificats  ###
##################################
EOF

  for alias in "${aliases[@]}"; do
    echo "Generate keypair and cert for ${alias}"

    keytool -genkeypair -keyalg RSA \
      -alias "${alias}" \
      -dname CN="${alias}" \
      -keypass unsecure \
      -storepass unsecure \
      -keystore "keystore-${alias}.jks"

    keytool -exportcert \
      -alias "${alias}" \
      -file "${alias}.pem" \
      -rfc \
      -storepass unsecure \
      -keystore "keystore-${alias}.jks"
  done
}

import_each_cert_in_each_keystore() {
  echo "###  import all cert in each keystore  ###"

  for alias_keystore in "${aliases[@]}"; do
    for alias_cert in "${aliases[@]}"; do
      if [[ "${alias_keystore}" != "${alias_cert}" ]]; then
        echo "import ${alias_cert}.pem into keystore-${alias_keystore}.jks"
        keytool -importcert -v \
          -file "${alias_cert}.pem" \
          -alias "${alias_cert}" \
          -noprompt \
          -trustcacerts \
          -storepass unsecure \
          -keystore "keystore-${alias_keystore}.jks"
      fi
    done
  done
}

show_keystores() {
  cat <<EOF
##################################
###        Show results        ###
##################################
EOF

  for keystore in keystore*.jks; do
    echo "###  ${keystore}  ###"
    keytool -list -storepass unsecure -keystore "${keystore}"
    echo ""
  done
}

generate_multiple_keystores() {
  generate_keystores_and_certificate
  import_each_cert_in_each_keystore
  show_keystores
}

generate_one_for_all_keystore() {
  for alias in "${aliases[@]}"; do
    echo "Generate keypair and cert for ${alias}"
    keytool -genkeypair -keyalg RSA \
      -alias "${alias}" \
      -dname CN="${alias}" \
      -keypass unsecure \
      -storepass unsecure \
      -keystore "keystore.jks"
  done

  # Hack - to keep same docker-compose volume mount, we make copy of the keystore with each named
  for alias in "${aliases[@]}"; do
    cp "keystore.jks" "keystore-${alias}.jks"
  done
}

### main ###
aliases=(alert-engine-1 alert-engine-2 management-api gateway-1 gateway-2)
mkdir -p keystores

pushd keystores >/dev/null
  generate_multiple_keystores
#  generate_one_for_all_keystore
popd >/dev/null
