/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.util.Base64Utils;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * @author Myrle Krantz
 */
class Helpers {

  static <T> boolean instancePresent(final List<T> users, Function<T, String> getIdentifier,
      final String identifier) {
    return users.stream().map(getIdentifier).filter(i -> i.equals(identifier)).findAny().isPresent();
  }

  static String generateRandomIdentifier(final String prefix) {
    return prefix + Math.abs(new Random().nextInt());
  }

  static String encodePassword(final String password) {
    return Base64Utils.encodeToString(password.getBytes());
  }
}
