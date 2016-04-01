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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Specialized {@code JaxWsProxySelector} for Jax-Ws.
 *
 * <p>Jax-Ws does not load the default java Proxy parameters unless a ProxySelector is provided.
 */
public class JaxWsProxySelector extends ProxySelector {

  private static final String HTTP_PROXY_HOST = "http.proxyHost";
  private static final String HTTP_PROXY_PORT = "http.proxyPort";
  private static final String HTTP_PROXY_USER = "http.proxyUser";
  private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";

  private static final String HTTPS_PROXY_HOST = "https.proxyHost";
  private static final String HTTPS_PROXY_PORT = "https.proxyPort";
  private static final String HTTPS_PROXY_USER = "https.proxyUser";
  private static final String HTTPS_PROXY_PASSWORD = "https.proxyPassword";

  private static final String FTP_PROXY_HOST = "ftp.proxyHost";
  private static final String FTP_PROXY_PORT = "ftp.proxyPort";
  private static final String FTP_PROXY_USER = "ftp.proxyUser";
  private static final String FTP_PROXY_PASSWORD = "ftp.proxyPassword";

  private static final String SOCKS_PROXY_HOST = "socksProxyHost";
  private static final String SOCKS_PROXY_PORT = "socksProxyPort";
  private static final String SOCKS_PROXY_USER = "socksProxyHost";
  private static final String SOCKS_PROXY_PASSWORD = "socksProxyPassword";

  private ProxySelector delegate = null;

  /**
   * @param def the {@code ProxySelector}
   */
  public JaxWsProxySelector(ProxySelector def) {
    delegate = def;
  }

  /**
   * Selects the Proxies available to do the user authentication.
   */
  @Override
  public List<Proxy> select(URI uri) {
    Preconditions.checkArgument(uri != null, "URI can't be null.");

    List<Proxy> list = new ArrayList<Proxy>();
    String scheme = uri.getScheme().toLowerCase();
    
    if (scheme.equals("http")
        && System.getProperty(HTTP_PROXY_HOST) != null
        && System.getProperty(HTTP_PROXY_PORT) != null) {

      if (System.getProperty(HTTP_PROXY_USER) != null
          && System.getProperty(HTTP_PROXY_USER).length() > 0) {
        Authenticator.setDefault(new JaxWsProxyAuthenticator());
      }
      list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty(HTTP_PROXY_HOST),
          Integer.parseInt(System.getProperty(HTTP_PROXY_PORT)))));
    }

    if (scheme.equals("https")
        && System.getProperty(HTTPS_PROXY_HOST) != null
        && System.getProperty(HTTPS_PROXY_PORT) != null) {

      if (System.getProperty(HTTP_PROXY_USER) != null
          && System.getProperty(HTTP_PROXY_USER).length() > 0) {
        Authenticator.setDefault(new JaxWsProxyAuthenticator());
      }
      list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
          System.getProperty(HTTPS_PROXY_HOST),
          Integer.parseInt(System.getProperty(HTTPS_PROXY_PORT)))));
    }

    if (scheme.equals("ftp")
        && System.getProperty(FTP_PROXY_HOST) != null
        && System.getProperty(FTP_PROXY_PORT) != null) {

      if (System.getProperty(FTP_PROXY_USER) != null
          && System.getProperty(FTP_PROXY_USER).length() > 0) {
        Authenticator.setDefault(new JaxWsProxyAuthenticator());
      }
      list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty(FTP_PROXY_HOST),
          Integer.parseInt(System.getProperty(FTP_PROXY_PORT)))));
    }

    if ((scheme.startsWith("http") || scheme.equals("ftp") || scheme.startsWith("socks"))
        && System.getProperty(SOCKS_PROXY_HOST) != null
        && System.getProperty(SOCKS_PROXY_PORT) != null) {

      if (System.getProperty(SOCKS_PROXY_USER) != null
          && System.getProperty(SOCKS_PROXY_USER).length() > 0) {
        Authenticator.setDefault(new JaxWsProxyAuthenticator());
      }
      list.add(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(
          System.getProperty(SOCKS_PROXY_HOST),
          Integer.parseInt(System.getProperty(SOCKS_PROXY_PORT)))));
    }

    if (list.size() > 0) {
      return list;
    }
    return this.delegateSelect(uri);
  }

  /**
   * Delegates the select to the embbeded selector.
   *
   * @param uri the {@code URI}
   * @return the list of proxies
   */
  private List<Proxy> delegateSelect(URI uri) {
    if (this.delegate != null) {
      return this.delegate.select(uri);
    } else {
      return Lists.newArrayList(Proxy.NO_PROXY);
    }
  }

  /**
   * Handles the connection failing when connecting to the Proxy. In case of failure, an
   * {@code IllegalArgumentException} is thrown.
   */
  @Override
  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    Preconditions.checkArgument(uri != null, "Argument \"uri\" cannot be null.");
    Preconditions.checkArgument(sa != null, "Argument \"sa\" cannot be null.");
    Preconditions.checkArgument(ioe != null, "Argument \"ioe\" cannot be null.");
  }

  /**
   * {@code Authenticator} to set username and password used in the proxy authentication.
   */
  private static final class JaxWsProxyAuthenticator extends Authenticator {
    /**
     * Does the authentication against the Proxy providing the username and the password.
     */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      if (System.getProperty(HTTP_PROXY_USER) != null
          && System.getProperty(HTTP_PROXY_USER).length() > 0) {
        return new PasswordAuthentication(
            System.getProperty(HTTP_PROXY_USER),
            System.getProperty(HTTP_PROXY_PASSWORD).toCharArray());
      } else if (System.getProperty(HTTPS_PROXY_USER) != null
          && System.getProperty(HTTPS_PROXY_USER).length() > 0) {
        return new PasswordAuthentication(
            System.getProperty(HTTPS_PROXY_USER),
            System.getProperty(HTTPS_PROXY_PASSWORD).toCharArray());
      } else if (System.getProperty(FTP_PROXY_USER) != null
          && System.getProperty(FTP_PROXY_USER).length() > 0) {
        return new PasswordAuthentication(
            System.getProperty(FTP_PROXY_USER),
            System.getProperty(FTP_PROXY_PASSWORD).toCharArray());
      } else {
        return new PasswordAuthentication(
            System.getProperty(SOCKS_PROXY_USER),
            System.getProperty(SOCKS_PROXY_PASSWORD).toCharArray());
      }
    }
  }
}
