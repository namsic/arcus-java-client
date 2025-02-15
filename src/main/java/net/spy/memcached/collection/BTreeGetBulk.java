/*
 * arcus-java-client : Arcus Java client
 * Copyright 2010-2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.spy.memcached.collection;

import java.util.List;

import net.spy.memcached.MemcachedNode;

public interface BTreeGetBulk<T> {

  int HEADER_COUNT = 3;

  void setKeySeparator(String keySeparator);

  String getSpaceSeparatedKeys();

  MemcachedNode getMemcachedNode();

  List<String> getKeyList();

  String stringify();

  String getCommand();

  boolean headerReady(int spaceCount);

  Object getBkey();

  int getDataLength();

  byte[] getEFlag();

  void decodeItemHeader(String[] splited);

  BTreeGetBulk<T> clone(MemcachedNode node, List<String> keyList);
}
