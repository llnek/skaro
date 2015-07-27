// This library is distributed in  the hope that it will be useful but without
// any  warranty; without  even  the  implied  warranty of  merchantability or
// fitness for a particular purpose.
// The use and distribution terms for this software are covered by the Eclipse
// Public License 1.0  (http://opensource.org/licenses/eclipse-1.0.php)  which
// can be found in the file epl-v10.html at the root of this distribution.
// By using this software in any  fashion, you are agreeing to be bound by the
// terms of this license. You  must not remove this notice, or any other, from
// this software.
// Copyright (c) 2013-2015, Ken Leung. All rights reserved.

package com.zotohlab.frwk.crypto;

import static com.zotohlab.frwk.util.CU.nsb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zotohlab.frwk.io.XStream;

/**
 *
 * @author kenl
 *
 */
public class SDataSource implements DataSource {

  private String _ctype= "";
  private byte[] _bits;
  private File _fn;

  /**
   * @param content
   * @param contentType
   */
  public SDataSource(File content, String contentType) {
    _ctype= nsb(contentType);
    _fn= content;
  }

  /**
   * @param content
   * @param contentType
   */
  public SDataSource(byte[] content, String contentType) {
    _ctype= nsb(contentType);
    _bits= content;
  }

  /**
   * @param content
   */
  public SDataSource(File content) {
    this(content, "");
  }

  /**
   * @param content
   */
  public SDataSource(byte[] content) {
    this(content, "");
  }

  public String getContentType() { return _ctype; }

  public InputStream getInputStream() {
    return (_fn==null) ? new ByteArrayInputStream(_bits) : new XStream(_fn);
  }

  public String getName() { return "Unknown"; }

  public OutputStream getOutputStream() throws IOException {
    throw new IOException("Not implemented");
  }

}

