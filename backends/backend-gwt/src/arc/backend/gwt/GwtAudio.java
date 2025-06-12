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

import arc.Core;
import arc.audio.Music;
import arc.audio.Sound;
import arc.backend.gwt.webaudio.WebAudioAPIManager;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import com.google.gwt.user.client.Timer;

public class GwtAudio {
	private WebAudioAPIManager webAudioAPIManager = null;

	private ObjectMap<String, String> outputDeviceLabelsIds = new ObjectMap<>();

	public GwtAudio () {
		webAudioAPIManager = new WebAudioAPIManager();

		if (((GwtApplication) Core.app).config.fetchAvailableOutputDevices) {
			getUserMedia();
			Timer observer = new Timer() {
				@Override
				public void run () {
					fetchAvailableOutputDevices(new DeviceListener() {
						@Override
						public void onDevicesChanged (String[] ids, String[] labels) {
							outputDeviceLabelsIds.clear();
							for (int i = 0; i < ids.length; i++) {
								outputDeviceLabelsIds.put(labels[i], ids[i]);
							}
						}
					});
				}
			};
			observer.scheduleRepeating(1000);
		}
	}

	public Sound newSound (Fi fileHandle) {
		return webAudioAPIManager.createSound(fileHandle);
	}

	public Music newMusic (Fi file) {
		return webAudioAPIManager.createMusic(file);
	}

	public boolean switchOutputDevice (String label) {
		String[] features = GwtFeaturePolicy.features();
		if (features == null || !Seq.with(features).contains("speaker-selection", false)
			|| GwtFeaturePolicy.allowsFeature("speaker-selection")) {
			String deviceIdentifier;
			if (label == null) {
				deviceIdentifier = ""; // Empty = default
			} else {
				deviceIdentifier = outputDeviceLabelsIds.get(label);
			}
			webAudioAPIManager.setSinkId(deviceIdentifier);
			return true;
		}
		return false;
	}

	private native void getUserMedia () /*-{
		navigator.mediaDevices.getUserMedia({ audio: true });
	}-*/;

	private native void fetchAvailableOutputDevices (DeviceListener listener) /*-{
		navigator.mediaDevices
			.enumerateDevices()
			.then(function(devices) {
				var dev = devices.filter(function(device) {
					return device.deviceId && device.kind === 'audiooutput' && device.deviceId !== 'default';
				})
				var ids = @com.badlogic.gdx.backends.gwt.GwtUtils::toStringArray(Lcom/google/gwt/core/client/JsArrayString;)(dev.map(function(device) {
					return device.deviceId;
				}));
				var labels = @com.badlogic.gdx.backends.gwt.GwtUtils::toStringArray(Lcom/google/gwt/core/client/JsArrayString;)(dev.map(function(device) {
					return device.label;
				}));
				listener.@com.badlogic.gdx.backends.gwt.DefaultGwtAudio.DeviceListener::onDevicesChanged([Ljava/lang/String;[Ljava/lang/String;)(ids, labels);
			});
	}-*/;

	private interface DeviceListener {
		void onDevicesChanged (String[] ids, String[] labels);
	}
}
