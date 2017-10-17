/**
 * Copyright 2017 Confluent Inc.
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
 **/

package io.confluent.ksql.structured;

import io.confluent.ksql.function.FunctionRegistry;
import io.confluent.ksql.function.UdafAggregator;
import io.confluent.ksql.parser.tree.KsqlWindowExpression;
import io.confluent.ksql.parser.tree.WindowExpression;
import io.confluent.ksql.GenericRow;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.WindowStore;

import java.util.List;

public class SchemaKGroupedStream {

  private final Schema schema;
  private final KGroupedStream kgroupedStream;
  private final Field keyField;
  private final List<SchemaKStream> sourceSchemaKStreams;
  private final FunctionRegistry functionRegistry;

  SchemaKGroupedStream(final Schema schema, final KGroupedStream kgroupedStream,
                       final Field keyField,
                       final List<SchemaKStream> sourceSchemaKStreams,
                       final FunctionRegistry functionRegistry) {
    this.schema = schema;
    this.kgroupedStream = kgroupedStream;
    this.keyField = keyField;
    this.sourceSchemaKStreams = sourceSchemaKStreams;
    this.functionRegistry = functionRegistry;
  }

  @SuppressWarnings("unchecked")
  public SchemaKTable aggregate(final Initializer initializer,
                                final UdafAggregator aggregator,
                                final WindowExpression windowExpression,
                                final Serde<GenericRow> topicValueSerDe,
                                final String storeName) {
    final KTable aggKtable;
    if (windowExpression != null) {
      final Materialized<String, GenericRow, ?> materialized
          = Materialized.<String, GenericRow, WindowStore<Bytes, byte[]>>as(storeName)
          .withValueSerde(topicValueSerDe);

      final KsqlWindowExpression ksqlWindowExpression = windowExpression.getKsqlWindowExpression();
      aggKtable = ksqlWindowExpression.applyAggregate(kgroupedStream, initializer, aggregator, materialized);
    } else {
      aggKtable = kgroupedStream.aggregate(initializer, aggregator, Materialized.with(null, topicValueSerDe));
    }
    return new SchemaKTable(schema, aggKtable, keyField, sourceSchemaKStreams, windowExpression != null,
                            SchemaKStream.Type.AGGREGATE, functionRegistry);

  }

}
