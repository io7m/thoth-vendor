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

package com.io7m.thoth.tests.command.vendor;

import com.io7m.thoth.command.vendor.VDatabaseRandomType;
import com.io7m.thoth.command.vendor.VDatabaseType;
import com.io7m.thoth.command.vendor.VProduct;
import com.io7m.thoth.command.vendor.VProductStatus;
import javaslang.control.Validation;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.hamcrest.core.StringContains;
import org.joda.money.Money;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public abstract class VDatabaseContract
{
  protected abstract VDatabaseType createDatabase(
    final VDatabaseRandomType random)
    throws Exception;

  @Test
  public void testEmpty(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());
    db.close();
  }

  @Test
  public void testCreate(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("USD 1.0"));
    final Validation<String, BigInteger> result = db.productCreate(product);
    final VProductStatus status = db.products().get(result.get());
    Assert.assertEquals(product, status.product());
    Assert.assertEquals(BigInteger.ZERO, status.stock());
    Assert.assertEquals(BigInteger.ZERO, status.purchases());
    db.close();
  }

  @Test
  public void testCreateDeleteCreate(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("USD 1.0"));

    {
      final Validation<String, BigInteger> cr0 = db.productCreate(product);
      Assert.assertTrue(cr0.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));

      final Validation<String, BigInteger> cr1 = db.productCreate(product);
      Assert.assertTrue(cr1.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));
      Assert.assertTrue(db.products().containsKey(cr1.get()));

      final Validation<String, BigInteger> cr2 = db.productCreate(product);
      Assert.assertTrue(cr2.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));
      Assert.assertTrue(db.products().containsKey(cr1.get()));
      Assert.assertTrue(db.products().containsKey(cr2.get()));

      final Validation<String, Void> dr0 = db.productDelete(cr0.get());
      Assert.assertTrue(dr0.isValid());
      Assert.assertFalse(db.products().containsKey(cr0.get()));
      Assert.assertTrue(db.products().containsKey(cr1.get()));
      Assert.assertTrue(db.products().containsKey(cr2.get()));

      final Validation<String, Void> dr1 = db.productDelete(cr1.get());
      Assert.assertTrue(dr1.isValid());
      Assert.assertFalse(db.products().containsKey(cr0.get()));
      Assert.assertFalse(db.products().containsKey(cr1.get()));
      Assert.assertTrue(db.products().containsKey(cr2.get()));

      final Validation<String, Void> dr2 = db.productDelete(cr2.get());
      Assert.assertTrue(dr2.isValid());
      Assert.assertFalse(db.products().containsKey(cr0.get()));
      Assert.assertFalse(db.products().containsKey(cr1.get()));
      Assert.assertFalse(db.products().containsKey(cr2.get()));
    }

    {
      final Validation<String, BigInteger> cr0 = db.productCreate(product);
      Assert.assertTrue(cr0.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));

      final Validation<String, BigInteger> cr1 = db.productCreate(product);
      Assert.assertTrue(cr1.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));
      Assert.assertTrue(db.products().containsKey(cr1.get()));

      final Validation<String, BigInteger> cr2 = db.productCreate(product);
      Assert.assertTrue(cr2.isValid());
      Assert.assertTrue(db.products().containsKey(cr0.get()));
      Assert.assertTrue(db.products().containsKey(cr1.get()));
      Assert.assertTrue(db.products().containsKey(cr2.get()));
    }

    db.close();
  }

  @Test
  public void testDeleteNonexistent(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final Validation<String, Void> result1 =
      db.productDelete(BigInteger.TEN);
    Assert.assertFalse(result1.isValid());

    db.close();
  }

  @Test
  public void testPurchaseOutOfStock(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("USD 1.0"));
    final Validation<String, BigInteger> result = db.productCreate(product);
    final BigInteger id = result.get();

    final Validation<String, String> purchase_result =
      db.productPurchase("someone", id);
    Assert.assertFalse(purchase_result.isValid());
    Assert.assertThat(
      purchase_result.getError(),
      StringContains.containsString("out of stock"));

    final VProductStatus status = db.products().get(id);
    Assert.assertEquals(product, status.product());
    Assert.assertEquals(BigInteger.ZERO, status.stock());
    Assert.assertEquals(BigInteger.ZERO, status.purchases());
    db.close();
  }

  @Test
  public void testPurchaseNonexistent(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("USD 1.0"));

    final Validation<String, String> purchase_result =
      db.productPurchase("someone", BigInteger.ONE);
    Assert.assertFalse(purchase_result.isValid());
    Assert.assertThat(
      purchase_result.getError(),
      StringContains.containsString("No such product"));

    db.close();
  }

  @Test
  public void testPurchaseRandomFailure(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("JPY 1.0"));
    final Validation<String, BigInteger> result = db.productCreate(product);
    final BigInteger id = result.get();
    db.productAddStock(id, 10);

    new StrictExpectations()
    {{
      random.randomFailure();
      this.result = Boolean.TRUE;
    }};

    final Validation<String, String> purchase_result =
      db.productPurchase("someone", id);
    Assert.assertFalse(purchase_result.isValid());
    Assert.assertThat(
      purchase_result.getError(),
      StringContains.containsString("machine makes a grinding noise"));

    db.close();
  }

  @Test
  public void testPurchaseOK(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final VProduct product = VProduct.of("Bread", Money.parse("JPY 1.0"));
    final Validation<String, BigInteger> result = db.productCreate(product);
    final BigInteger id = result.get();
    db.productAddStock(id, 10);

    new StrictExpectations()
    {{
      random.randomFailure();
      this.result = Boolean.FALSE;
      random.randomFailure();
      this.result = Boolean.FALSE;
    }};

    {
      final Validation<String, String> purchase_result =
        db.productPurchase("someone", id);
      Assert.assertTrue(purchase_result.isValid());
    }

    {
      final VProductStatus status = db.products().get(id);
      Assert.assertEquals(product, status.product());
      Assert.assertEquals(BigInteger.valueOf(9L), status.stock());
      Assert.assertEquals(BigInteger.valueOf(1L), status.purchases());
    }

    {
      final Validation<String, String> purchase_result =
        db.productPurchase("someone", id);
      Assert.assertTrue(purchase_result.isValid());
    }

    {
      final VProductStatus status = db.products().get(id);
      Assert.assertEquals(product, status.product());
      Assert.assertEquals(BigInteger.valueOf(8L), status.stock());
      Assert.assertEquals(BigInteger.valueOf(2L), status.purchases());
    }

    db.close();
  }

  @Test
  public void testAddStockNonexistent(
    final @Mocked VDatabaseRandomType random)
    throws Exception
  {
    final VDatabaseType db = this.createDatabase(random);
    Assert.assertTrue(db.products().isEmpty());

    final Validation<String, BigInteger> purchase_result =
      db.productAddStock(BigInteger.ONE, 10);
    Assert.assertFalse(purchase_result.isValid());
    Assert.assertThat(
      purchase_result.getError(),
      StringContains.containsString("No such product"));

    db.close();
  }
}
