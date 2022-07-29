/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.site.permissions.impl

import android.webkit.PermissionRequest
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.site.permissions.api.SitePermissionsManager
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

@ContributesBinding(ActivityScope::class)
class SitePermissionsManagerImpl @Inject constructor(
    private val sitePermissionsRepository: SitePermissionsRepository
) : SitePermissionsManager {

    override fun getSitePermissionsGranted(url: String, tabId: String, resources: Array<String>): Array<String> =
        resources
            .filter { sitePermissionsRepository.isDomainGranted(url, tabId, it) }
            .toTypedArray()

    override fun getSitePermissionsAllowedToAsk(url: String, resources: Array<String>): Array<String> =
        resources
            .filter { isPermissionSupported(it) }
            .filter { sitePermissionsRepository.isDomainAllowedToAsk(url, it) }
            .toTypedArray()


    private fun isPermissionSupported(permission: String): Boolean =
        permission == PermissionRequest.RESOURCE_AUDIO_CAPTURE || permission == PermissionRequest.RESOURCE_VIDEO_CAPTURE

}
