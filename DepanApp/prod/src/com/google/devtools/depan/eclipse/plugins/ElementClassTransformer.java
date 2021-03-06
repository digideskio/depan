/*
 * Copyright 2008 The Depan Project Authors
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

package com.google.devtools.depan.eclipse.plugins;

import com.google.devtools.depan.model.Element;

/**
 * An {@link ElementTransformer} that uses the {@link Class} of the element
 * to give the result.
 *
 * @author Yohann Coppel
 *
 * @param <R> type for the transformation result.
 */
public interface ElementClassTransformer<R> {

  /**
   * Associate a R to an {@link Element} class.
   * @param element
   * @return the result of the transformation.
   */
  R transform(Class<? extends Element> element);
}
