/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.thoth.command.vendor;

import javaslang.control.Validation;
import org.joda.money.Money;
import org.osgi.annotation.versioning.ProviderType;

import java.io.Closeable;
import java.math.BigInteger;
import java.util.SortedMap;

@ProviderType
public interface VDatabaseType extends Closeable
{
  SortedMap<BigInteger, VProductStatus> products();

  SortedMap<String, Money> accounting();

  Validation<String, Void> productDelete(BigInteger id);

  Validation<String, BigInteger> productCreate(VProduct product);

  Validation<String, BigInteger> productAddStock(BigInteger id, int count);

  Validation<String, String> productPurchase(String owner, BigInteger id);
}
