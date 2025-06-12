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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import arc.Core;
import arc.Files;
import arc.backend.gwt.preloader.Preloader;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

public class GwtFi extends Fi {
	public final Preloader preloader;
	private final String file;
	private final Files.FileType type;

	public GwtFi(Preloader preloader, String fileName, Files.FileType type) {
		if (type != Files.FileType.internal && type != Files.FileType.classpath)
			throw new ArcRuntimeException("FileType '" + type + "' Not supported in GWT backend");
		this.preloader = preloader;
		this.file = fixSlashes(fileName);
		this.type = type;
	}

	public GwtFi(String path) {
		this.type = Files.FileType.internal;
		this.preloader = ((GwtApplication) Core.app).getPreloader();
		this.file = fixSlashes(path);
	}

	/** @return The full url to an asset, e.g. http://localhost:8080/assets/data/shotgun-e5f56587d6f025bff049632853ae4ff9.ogg */
	public String getAssetUrl () {
		return preloader.baseUrl + preloader.assetNames.get(file, file);
	}

	public String path () {
		return file;
	}

	public String name () {
		int index = file.lastIndexOf('/');
		if (index < 0) return file;
		return file.substring(index + 1);
	}

	public String extension () {
		String name = name();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1) return "";
		return name.substring(dotIndex + 1);
	}

	public String nameWithoutExtension () {
		String name = name();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1) return name;
		return name.substring(0, dotIndex);
	}

	/** @return the path and filename without the extension, e.g. dir/dir2/file.png -> dir/dir2/file */
	public String pathWithoutExtension () {
		String path = file;
		int dotIndex = path.lastIndexOf('.');
		if (dotIndex == -1) return path;
		return path.substring(0, dotIndex);
	}

	public Files.FileType type () {
		return type;
	}

	/** Returns a java.io.File that represents this file handle. Note the returned file will only be usable for
	 * {@link Files.FileType#absolute} and {@link Files.FileType#external} file handles. */
	public File file () {
		throw new ArcRuntimeException("file() not supported in GWT backend");
	}

	/** Returns a stream for reading this file as bytes.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public InputStream read () {
		InputStream in = preloader.read(file);
		if (in == null) throw new ArcRuntimeException(file + " does not exist");
		return in;
	}

	/** Returns a buffered stream for reading this file as bytes.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public BufferedInputStream read (int bufferSize) {
		return new BufferedInputStream(read(), bufferSize);
	}

	/** Returns a reader for reading this file as characters.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public Reader reader () {
		return new InputStreamReader(read());
	}

	/** Returns a reader for reading this file as characters.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public Reader reader (String charset) {
		try {
			return new InputStreamReader(read(), charset);
		} catch (UnsupportedEncodingException e) {
			throw new ArcRuntimeException("Encoding '" + charset + "' not supported", e);
		}
	}

	/** Returns a buffered reader for reading this file as characters.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public BufferedReader reader (int bufferSize) {
		return new BufferedReader(reader(), bufferSize);
	}

	/** Returns a buffered reader for reading this file as characters.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public BufferedReader reader (int bufferSize, String charset) {
		return new BufferedReader(reader(charset), bufferSize);
	}

	/** Reads the entire file into a string using the platform's default charset.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public String readString () {
		return readString(null);
	}

	/** Reads the entire file into a string using the specified charset.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public String readString (String charset) {
		if (preloader.isText(file)) return preloader.texts.get(file);
		return new String(readBytes(), StandardCharsets.UTF_8);
	}

	/** Reads the entire file into a byte array.
	 * @throws ArcRuntimeException if the file handle represents a directory, doesn't exist, or could not be read. */
	public byte[] readBytes () {
		int length = (int)length();
		if (length == 0) length = 512;
		byte[] buffer = new byte[length];
		int position = 0;
		try (InputStream input = read()) {
			while (true) {
				int count = input.read(buffer, position, buffer.length - position);
				if (count == -1) break;
				position += count;
				if (position == buffer.length) {
					// Grow buffer.
					byte[] newBuffer = new byte[buffer.length * 2];
					System.arraycopy(buffer, 0, newBuffer, 0, position);
					buffer = newBuffer;
				}
			}
		} catch (IOException ex) {
			throw new ArcRuntimeException("Error reading file: " + this, ex);
		}
		if (position < buffer.length) {
			// Shrink buffer.
			byte[] newBuffer = new byte[position];
			System.arraycopy(buffer, 0, newBuffer, 0, position);
			buffer = newBuffer;
		}
		return buffer;
	}

	/** Reads the entire file into the byte array. The byte array must be big enough to hold the file's data.
	 * @param bytes the array to load the file into
	 * @param offset the offset to start writing bytes
	 * @param size the number of bytes to read, see {@link #length()}
	 * @return the number of read bytes */
	public int readBytes (byte[] bytes, int offset, int size) {
		InputStream input = read();
		int position = 0;
		try {
			while (true) {
				int count = input.read(bytes, offset + position, size - position);
				if (count <= 0) break;
				position += count;
			}
		} catch (IOException ex) {
			throw new ArcRuntimeException("Error reading file: " + this, ex);
		} finally {
			try {
				if (input != null) input.close();
			} catch (IOException ignored) {
			}
		}
		return position - offset;
	}

	public ByteBuffer map () {
		throw new ArcRuntimeException("Cannot map files in GWT backend");
	}

	public ByteBuffer map (FileChannel.MapMode mode) {
		throw new ArcRuntimeException("Cannot map files in GWT backend");
	}

	/** Returns a stream for writing to this file. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public OutputStream write (boolean append) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Reads the remaining bytes from the specified stream and writes them to this file. The stream is closed. Parent directories
	 * will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public void write (InputStream input, boolean append) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Returns a writer for writing to this file using the default charset. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public Writer writer (boolean append) {
		return writer(append, null);
	}

	/** Returns a writer for writing to this file. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @param charset May be null to use the default charset.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public Writer writer (boolean append, String charset) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Writes the specified string to the file using the default charset. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public void writeString (String string, boolean append) {
		writeString(string, append, null);
	}

	/** Writes the specified string to the file as UTF-8. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @param charset May be null to use the default charset.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public void writeString (String string, boolean append, String charset) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Writes the specified bytes to the file. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public void writeBytes (byte[] bytes, boolean append) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Writes the specified bytes to the file. Parent directories will be created if necessary.
	 * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
	 * @throws ArcRuntimeException if this file handle represents a directory, if it is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file, or if it could not be written. */
	public void writeBytes (byte[] bytes, int offset, int length, boolean append) {
		throw new ArcRuntimeException("Cannot write to files in GWT backend");
	}

	/** Returns the paths to the children of this directory. Returns an empty list if this file handle represents a file and not a
	 * directory. On the desktop, an {@link Files.FileType#internal} handle to a directory on the classpath will return a zero length
	 * array.
	 * @throws ArcRuntimeException if this file is an {@link Files.FileType#classpath} file. */
	public Fi[] list () {
		return preloader.list(file);
	}

	/** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
	 * handle represents a file and not a directory. On the desktop, an {@link Files.FileType#internal} handle to a directory on the
	 * classpath will return a zero length array.
	 * @throws ArcRuntimeException if this file is an {@link Files.FileType#classpath} file. */
	public Fi[] list (FileFilter filter) {
		return preloader.list(file, filter);
	}

	/** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
	 * handle represents a file and not a directory. On the desktop, an {@link Files.FileType#internal} handle to a directory on the
	 * classpath will return a zero length array.
	 * @throws ArcRuntimeException if this file is an {@link Files.FileType#classpath} file. */
	public Fi[] list (FilenameFilter filter) {
		return preloader.list(file, filter);
	}

	/** Returns the paths to the children of this directory with the specified suffix. Returns an empty list if this file handle
	 * represents a file and not a directory. On the desktop, an {@link Files.FileType#internal} handle to a directory on the classpath
	 * will return a zero length array.
	 * @throws ArcRuntimeException if this file is an {@link Files.FileType#classpath} file. */
	public Fi[] list (String suffix) {
		return preloader.list(file, suffix);
	}

	/** Returns true if this file is a directory. Always returns false for classpath files. On Android, an
	 * {@link Files.FileType#internal} handle to an empty directory will return false. On the desktop, an {@link Files.FileType#internal}
	 * handle to a directory on the classpath will return false. */
	public boolean isDirectory () {
		return preloader.isDirectory(file);
	}

	/**
	 * Returns a handle to the child with the specified name.
	 *
	 * @throws ArcRuntimeException if this file handle is a {@link Files.FileType#classpath} or {@link Files.FileType#internal} and the child
	 *                             doesn't exist.
	 */
	public Fi child (String name) {
		return new GwtFi(preloader, (file.isEmpty() ? "" : (file + (file.endsWith("/") ? "" : "/"))) + name,
			Files.FileType.internal);
	}

	public Fi parent () {
		int index = file.lastIndexOf("/");
		String dir = "";
		if (index > 0) dir = file.substring(0, index);
		return new GwtFi(preloader, dir, type);
	}

	public Fi sibling (String name) {
		return parent().child(fixSlashes(name));
	}

	/** @throws ArcRuntimeException if this file handle is a {@link Files.FileType#classpath} or {@link Files.FileType#internal} file. */
	public void mkdirs () {
		throw new ArcRuntimeException("Cannot mkdirs with an internal file: " + file);
	}

	/** Returns true if the file exists. On Android, a {@link Files.FileType#classpath} or {@link Files.FileType#internal} handle to a
	 * directory will always return false. */
	public boolean exists () {
		return preloader.contains(file);
	}

	/** Deletes this file or empty directory and returns success. Will not delete a directory that has children.
	 * @throws ArcRuntimeException if this file handle is a {@link Files.FileType#classpath} or {@link Files.FileType#internal} file. */
	public boolean delete () {
		throw new ArcRuntimeException("Cannot delete an internal file: " + file);
	}

	/** Deletes this file or directory and all children, recursively.
	 * @throws ArcRuntimeException if this file handle is a {@link Files.FileType#classpath} or {@link Files.FileType#internal} file. */
	public boolean deleteDirectory () {
		throw new ArcRuntimeException("Cannot delete an internal file: " + file);
	}

	/** Copies this file or directory to the specified file or directory. If this handle is a file, then 1) if the destination is a
	 * file, it is overwritten, or 2) if the destination is a directory, this file is copied into it, or 3) if the destination
	 * doesn't exist, {@link #mkdirs()} is called on the destination's parent and this file is copied into it with a new name. If
	 * this handle is a directory, then 1) if the destination is a file, ArcRuntimeException is thrown, or 2) if the destination is
	 * a directory, this directory is copied into it recursively, overwriting existing files, or 3) if the destination doesn't
	 * exist, {@link #mkdirs()} is called on the destination and this directory is copied into it recursively.
	 * @throws ArcRuntimeException if the destination file handle is a {@link Files.FileType#classpath} or {@link Files.FileType#internal}
	 *            file, or copying failed. */
	public void copyTo (Fi dest) {
		throw new ArcRuntimeException("Cannot copy to an internal file: " + dest);
	}

	/** Moves this file to the specified file, overwriting the file if it already exists.
	 * @throws ArcRuntimeException if the source or destination file handle is a {@link Files.FileType#classpath} or
	 *            {@link Files.FileType#internal} file. */
	public void moveTo (Fi dest) {
		throw new ArcRuntimeException("Cannot move an internal file: " + file);
	}

	/** Returns the length in bytes of this file, or 0 if this file is a directory, does not exist, or the size cannot otherwise be
	 * determined. */
	public long length () {
		return preloader.length(file);
	}

	/** Returns the last modified time in milliseconds for this file. Zero is returned if the file doesn't exist. Zero is returned
	 * for {@link Files.FileType#classpath} files. On Android, zero is returned for {@link Files.FileType#internal} files. On the desktop, zero
	 * is returned for {@link Files.FileType#internal} files on the classpath. */
	public long lastModified () {
		return 0;
	}

	public String toString () {
		return file;
	}

	private static String fixSlashes (String path) {
		path = path.replace('\\', '/');
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
