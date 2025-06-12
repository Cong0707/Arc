/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package arc.backend.gwt;

import arc.Files;
import arc.backend.gwt.preloader.Preloader;
import arc.files.Fi;
import arc.util.ArcRuntimeException;
import com.google.gwt.storage.client.Storage;

public class GwtFiles implements Files {

	public static final Storage LocalStorage = Storage.getLocalStorageIfSupported(); // Can be null if cookies are disabled or
																												// blocked by the browser with "block
																												// third-party cookies"

	protected final Preloader preloader;

	public GwtFiles (Preloader preloader) {
		this.preloader = preloader;
	}

	@Override
	public Fi get(String path, FileType type) {
		return null;
	}

	@Override
	public Fi classpath (String path) {
		return new GwtFi(preloader, path, FileType.classpath);
	}

	@Override
	public Fi internal (String path) {
		return new GwtFi(preloader, path, FileType.internal);
	}

	@Override
	public Fi external (String path) {
		throw new ArcRuntimeException("external() not supported in GWT backend");
	}

	@Override
	public Fi absolute (String path) {
		throw new ArcRuntimeException("absolute() not supported in GWT backend");
	}

	@Override
	public Fi local (String path) {
		throw new ArcRuntimeException("local() not supported in GWT backend");
	}

	@Override
	public String getExternalStoragePath () {
		return null;
	}

	@Override
	public boolean isExternalStorageAvailable () {
		return false;
	}

	@Override
	public String getLocalStoragePath () {
		return null;
	}

	@Override
	public boolean isLocalStorageAvailable () {
		return false;
	}
}
