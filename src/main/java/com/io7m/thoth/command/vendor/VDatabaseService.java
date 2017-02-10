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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.SortedMap;

@Component(immediate = true)
public final class VDatabaseService implements VDatabaseType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(VDatabaseService.class);
  }

  private VDatabaseType database;

  public VDatabaseService()
  {

  }

  @Activate
  public void onActivate(
    final BundleContext context)
  {
    final File database_file = context.getDataFile("v1_database");
    LOG.debug("opening database file {}", database_file);
    database_file.delete();
    database_file.mkdirs();

    this.database =
      new VDatabaseTrivial(
        new VDatabaseRandomType()
        {
        },
        database_file);
  }

  @Deactivate
  public void onDeactivate()
    throws IOException
  {
    this.database.close();
  }

  @Override
  public SortedMap<BigInteger, VProductStatus> products()
  {
    return this.database.products();
  }

  @Override
  public SortedMap<String, Money> accounting()
  {
    return this.database.accounting();
  }

  @Override
  public Validation<String, Void> productDelete(
    final BigInteger id)
  {
    return this.database.productDelete(id);
  }

  @Override
  public Validation<String, BigInteger> productCreate(
    final VProduct product)
  {
    return this.database.productCreate(product);
  }

  @Override
  public Validation<String, BigInteger> productAddStock(
    final BigInteger id,
    final int count)
  {
    return this.database.productAddStock(id, count);
  }

  @Override
  public Validation<String, String> productPurchase(
    final String owner,
    final BigInteger id)
  {
    return this.database.productPurchase(owner, id);
  }

  @Override
  public void close()
    throws IOException
  {
    this.database.close();
  }
}
