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

import com.io7m.thoth.command.api.ThothCommandType;
import com.io7m.thoth.command.api.ThothResponse;
import javaslang.collection.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

@Component(immediate = true, service = ThothCommandType.class)
public final class VCommandList extends VCommand
{
  private final AtomicReference<VDatabaseType> database;

  public VCommandList()
  {
    super(VCommandList.class);
    this.database = new AtomicReference<>();
  }

  @Reference(
    cardinality = ReferenceCardinality.OPTIONAL,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onDatabaseServiceUnbind")
  public void onDatabaseServiceBind(
    final VDatabaseType database)
  {
    this.log().debug("database service became available");
    this.database.set(database);
  }

  public void onDatabaseServiceUnbind(
    final VDatabaseType database)
  {
    this.log().debug("database service became unavailable");
    this.database.set(database);
  }

  @Override
  public String name()
  {
    return "list";
  }

  @Override
  public List<ThothResponse> execute(
    final List<String> text)
  {
    final VDatabaseType db = this.database.get();
    if (db != null) {
      final SortedMap<BigInteger, VProductStatus> products = db.products();
      final java.util.List<ThothResponse> responses = new ArrayList<>(products.size());
      final String format = "%-3s | %-8s | %-5s | %s";
      responses.add(ThothResponse.of(String.format(
        format, "ID", "Price", "Stock", "Name")));
      products.forEach((id, status) -> {
        final VProduct product = status.product();
        responses.add(ThothResponse.of(String.format(
          format, id, product.price(), status.stock(), product.name())));
      });
      return List.ofAll(responses);
    }

    return List.of(ThothResponse.of("Vendor database is offline."));
  }
}
