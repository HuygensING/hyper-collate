package nl.knaw.huygens.hypercollate.api

/*
 * #%L
 * hyper-collate-api
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javax.ws.rs.core.MediaType

object UTF8MediaType {
    private const val CHARSET_UTF8 = "; charset=UTF-8"
    const val TEXT_PLAIN = MediaType.TEXT_PLAIN + CHARSET_UTF8
    const val TEXT_XML = MediaType.TEXT_XML + CHARSET_UTF8
    const val APPLICATION_JSON = MediaType.APPLICATION_JSON + CHARSET_UTF8
}
