package nl.knaw.huygens.hypercollate.api

/*-
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

import java.net.URI

class AboutInfo {
    private var appName: String? = null
    private var startedAt: String? = null
    var version: String? = null
        private set
    private var buildDate: String? = null
    private var commitId: String? = null
    private var scmBranch: String? = null
    private var projectDirURI: URI? = null
    private var isDotRendering = false

    fun setAppName(appName: String?): AboutInfo {
        this.appName = appName
        return this
    }

    fun setStartedAt(startedAt: String?): AboutInfo {
        this.startedAt = startedAt
        return this
    }

    fun setVersion(version: String?): AboutInfo {
        this.version = version
        return this
    }

    fun setBuildDate(buildDate: String?): AboutInfo {
        this.buildDate = buildDate
        return this
    }

    fun setCommitId(commitId: String?): AboutInfo {
        this.commitId = commitId
        return this
    }

    fun setScmBranch(scmBranch: String?): AboutInfo {
        this.scmBranch = scmBranch
        return this
    }

    fun setProjectDirURI(projectDir: URI?): AboutInfo {
        projectDirURI = projectDir
        return this
    }

    fun setDotRendering(dotRendering: Boolean): AboutInfo {
        isDotRendering = dotRendering
        return this
    }
}
