// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.ads.adwords.awalerting.util;

import com.google.api.ads.common.lib.utils.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Util class for File objects.
 */
public final class FileUtil {
  /**
   * Private constructor to prevent initialization.
   */
  private FileUtil() {}

  /**
   * GUnzips a source File to a destination File.
   *
   * @param srcFile the source file
   * @param destFile the destination file
   * @throws IOException error handling the files
   */
  public static void gUnzip(File srcFile, File destFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(destFile);
    InputStream fis = new FileInputStream(srcFile);
    GZIPInputStream zIn = new GZIPInputStream(fis);

    Streams.copy(zIn, fos);

    zIn.close();
    fis.close();
    fos.close();
  }

  /**
   * GZips a source File to a destination File.
   *
   * @param srcFile the source file
   * @param destFile the destination file
   * @throws IOException error handling the files
   */
  public static void gZip(File srcFile, File destFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(destFile);
    InputStream fis = new FileInputStream(srcFile);
    GZIPOutputStream zos = new GZIPOutputStream(fos);

    Streams.copy(fis, zos);
    zos.close();
    fis.close();
    fos.close();
  }
}
