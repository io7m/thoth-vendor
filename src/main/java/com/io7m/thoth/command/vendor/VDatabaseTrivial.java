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

import com.io7m.jnull.NullCheck;
import javaslang.control.Validation;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

public final class VDatabaseTrivial implements VDatabaseType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(VDatabaseTrivial.class);
  }

  private final VDatabaseRandomType random;
  private final TreeMap<BigInteger, VProductStatus> product_definitions;
  private final TreeMap<String, Money> purchases_cash;
  private final File product_definitions_file;
  private final File purchases_cash_file;

  @SuppressWarnings("unchecked")
  private static <T> T diskLoad(
    final File file,
    final Supplier<T> on_default)
  {
    if (file.isFile()) {
      try (final ObjectInputStream is = new ObjectInputStream(Files.newInputStream(
        file.toPath()))) {
        return (T) is.readObject();
      } catch (final IOException | ClassNotFoundException e) {
        LOG.error("could not load database: {}: ", file, e);
      }
    }
    return on_default.get();
  }

  private static <T> void diskSave(
    final File file,
    final T data)
  {
    final File file_tmp = new File(file.toString() + ".tmp");
    try (final ObjectOutputStream out =
           new ObjectOutputStream(Files.newOutputStream(
             file_tmp.toPath(),
             StandardOpenOption.TRUNCATE_EXISTING,
             StandardOpenOption.CREATE,
             StandardOpenOption.WRITE))) {
      out.writeObject(data);
      Files.move(
        file_tmp.toPath(),
        file.toPath(),
        StandardCopyOption.ATOMIC_MOVE);
    } catch (final IOException e) {
      LOG.error("i/o error: {}: ", file, e);
    }
  }

  private void diskCommit()
  {
    try {
      diskSave(this.product_definitions_file, this.product_definitions);
      diskSave(this.purchases_cash_file, this.purchases_cash);
    } catch (final Exception e) {
      LOG.error("commit error: ", e);
    }
  }

  @SuppressWarnings("unchecked")
  public VDatabaseTrivial(
    final VDatabaseRandomType in_random,
    final File in_directory)
  {
    this.random = NullCheck.notNull(in_random, "Random");
    NullCheck.notNull(in_directory, "Directory");

    this.product_definitions_file =
      new File(in_directory, "v1_products.db");
    this.product_definitions =
      diskLoad(this.product_definitions_file, TreeMap::new);
    this.purchases_cash_file =
      new File(in_directory, "v1_cash.db");
    this.purchases_cash =
      diskLoad(this.purchases_cash_file, TreeMap::new);
  }

  @Override
  public void close()
    throws IOException
  {
    this.diskCommit();
  }

  @Override
  public SortedMap<BigInteger, VProductStatus> products()
  {
    return Collections.unmodifiableSortedMap(this.product_definitions);
  }

  @Override
  public SortedMap<String, Money> accounting()
  {
    return Collections.unmodifiableSortedMap(this.purchases_cash);
  }

  @Override
  public Validation<String, Void> productDelete(
    final BigInteger id)
  {
    LOG.debug("product delete: {}", id);

    if (this.product_definitions.containsKey(id)) {
      this.product_definitions.remove(id);
      this.diskCommit();
      return valid(null);
    }

    return invalid("No such product: " + id);
  }

  @Override
  public Validation<String, BigInteger> productCreate(
    final VProduct product)
  {
    LOG.debug("product create: {}", product);

    final BigInteger next = this.freshID();
    this.product_definitions.put(
      next, VProductStatus.of(product, BigInteger.ZERO, BigInteger.ZERO));
    this.diskCommit();
    return valid(next);
  }

  private BigInteger freshID()
  {
    BigInteger id = BigInteger.ZERO;
    while (true) {
      if (!this.product_definitions.containsKey(id)) {
        return id;
      }
      id = id.add(BigInteger.ONE);
    }
  }

  @Override
  public Validation<String, BigInteger> productAddStock(
    final BigInteger id,
    final int count)
  {
    LOG.debug("product add stock: {} {}", id, Integer.valueOf(count));

    if (this.product_definitions.containsKey(id)) {
      final VProductStatus status = this.product_definitions.get(id);
      final VProductStatus new_status =
        status.withStock(BigInteger.valueOf(Math.max(0L, (long) count)));

      this.product_definitions.put(id, new_status);
      this.diskCommit();
      return valid(new_status.stock());
    }

    return invalid("No such product: " + id);
  }

  @Override
  public Validation<String, String> productPurchase(
    final String owner,
    final BigInteger id)
  {
    LOG.debug("product purchase: {} {}", owner, id);

    if (this.product_definitions.containsKey(id)) {
      final VProductStatus status = this.product_definitions.get(id);
      if (status.isInStock()) {
        final Money current = this.userGetCurrentPurchaseSum(owner);
        final Money new_current = current.plus(status.product().price());
        this.purchases_cash.put(owner, new_current);

        if (!this.random.randomFailure()) {
          final VProductStatus new_status = status
            .withPurchases(status.purchases().add(BigInteger.ONE))
            .withStock(status.stock().subtract(BigInteger.ONE));

          this.product_definitions.put(id, new_status);
          this.diskCommit();
          return valid("The machine dispenses " + status.product().name());
        }

        final VProductStatus new_status =
          status.withPurchases(status.purchases().add(BigInteger.ONE));

        this.product_definitions.put(id, new_status);
        this.diskCommit();
        return invalid("The machine makes a grinding noise.");
      }
      return invalid("The product is out of stock.");
    }

    return invalid("No such product: " + id);
  }

  private Money userGetCurrentPurchaseSum(
    final String owner)
  {
    if (this.purchases_cash.containsKey(owner)) {
      return this.purchases_cash.get(owner);
    }
    return Money.ofMajor(CurrencyUnit.JPY, 0L);
  }
}
