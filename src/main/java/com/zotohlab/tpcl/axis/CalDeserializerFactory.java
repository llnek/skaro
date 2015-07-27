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

package com.zotohlab.tpcl.axis;

import javax.xml.namespace.QName;

import org.apache.axis.encoding.ser.BaseDeserializerFactory;

/**
 * @author kenl
 */
public class CalDeserializerFactory extends BaseDeserializerFactory {

  private static final long serialVersionUID = 1L;


  public CalDeserializerFactory(Class<?> cz) {
    super(cz);
  }

  public CalDeserializerFactory(Class<?> java_type, QName xml_type) {
    this(CalDeserializer.class);
    javaType= java_type;
    xmlType= xml_type;
  }

}



