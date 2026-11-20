/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.endpoints

import org.mbari.oni.domain.UserAccount

/** Who made an authenticated request. See Endpoints.resolveCaller. */
enum Caller:
    /** A client using an API-key token. There is no user account; API-key holders are trusted. */
    case ApiKey

    /** A logged-in user, with their account as currently stored in the database (not as claimed by the token). */
    case User(account: UserAccount)
