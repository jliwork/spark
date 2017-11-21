/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.datasources

import org.apache.spark.sql.{sources, QueryTest}
import org.apache.spark.sql.catalyst.expressions
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.test.SharedSQLContext
import org.apache.spark.sql.types._


class DataSourceStrategySuite extends QueryTest with SharedSQLContext {

  test("translate simple expression") {
    val attrInt = AttributeReference("cint", IntegerType)()
    val attrStr = AttributeReference("cstr", StringType)()

    assertResult(Some(sources.EqualTo("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.EqualTo(attrInt, Literal(1)))
    }
    assertResult(Some(sources.EqualTo("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.EqualTo(Literal(1), attrInt))
    }

    assertResult(Some(sources.EqualNullSafe("cstr", null))) {
      DataSourceStrategy.translateFilter(
        expressions.EqualNullSafe(attrStr, Literal(null)))
    }
    assertResult(Some(sources.EqualNullSafe("cstr", null))) {
      DataSourceStrategy.translateFilter(
        expressions.EqualNullSafe(Literal(null), attrStr))
    }

    assertResult(Some(sources.GreaterThan("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.GreaterThan(attrInt, Literal(1)))
    }
    assertResult(Some(sources.GreaterThan("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.LessThan(Literal(1), attrInt))
    }

    assertResult(Some(sources.LessThan("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.LessThan(attrInt, Literal(1)))
    }
    assertResult(Some(sources.LessThan("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.GreaterThan(Literal(1), attrInt))
    }

    assertResult(Some(sources.GreaterThanOrEqual("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.GreaterThanOrEqual(attrInt, Literal(1)))
    }
    assertResult(Some(sources.GreaterThanOrEqual("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.LessThanOrEqual(Literal(1), attrInt))
    }

    assertResult(Some(sources.LessThanOrEqual("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.LessThanOrEqual(attrInt, Literal(1)))
    }
    assertResult(Some(sources.LessThanOrEqual("cint", 1))) {
      DataSourceStrategy.translateFilter(
        expressions.GreaterThanOrEqual(Literal(1), attrInt))
    }

    assertResult(Some(sources.In("cint", Array(1, 2, 3)))) {
      DataSourceStrategy.translateFilter(
        expressions.InSet(attrInt, Set(1, 2, 3)))
    }

    assertResult(Some(sources.In("cint", Array(1, 2, 3)))) {
      DataSourceStrategy.translateFilter(
        expressions.In(attrInt, Seq(Literal(1), Literal(2), Literal(3))))
    }

    assertResult(Some(sources.IsNull("cint"))) {
      DataSourceStrategy.translateFilter(
        expressions.IsNull(attrInt))
    }
    assertResult(Some(sources.IsNotNull("cint"))) {
      DataSourceStrategy.translateFilter(
        expressions.IsNotNull(attrInt))
    }

    assertResult(Some(sources.And(
      sources.GreaterThan("cint", 1),
      sources.LessThan("cint", 10)))) {
      DataSourceStrategy.translateFilter(expressions.And(
        expressions.GreaterThan(attrInt, Literal(1)),
        expressions.LessThan(attrInt, Literal(10))
      ))
    }

    assertResult(Some(sources.Or(
      sources.GreaterThanOrEqual("cint", 8),
      sources.LessThanOrEqual("cint", 2)))) {
      DataSourceStrategy.translateFilter(expressions.Or(
        expressions.GreaterThanOrEqual(attrInt, Literal(8)),
        expressions.LessThanOrEqual(attrInt, Literal(2))
      ))
    }

    assertResult(Some(sources.Not(
      sources.GreaterThanOrEqual("cint", 8)))) {
      DataSourceStrategy.translateFilter(
        expressions.Not(expressions.GreaterThanOrEqual(attrInt, Literal(8))
        ))
    }

    assertResult(Some(sources.StringStartsWith("cstr", "a"))) {
      DataSourceStrategy.translateFilter(
        expressions.StartsWith(attrStr, Literal("a")
        ))
    }

    assertResult(Some(sources.StringEndsWith("cstr", "a"))) {
      DataSourceStrategy.translateFilter(
        expressions.EndsWith(attrStr, Literal("a")
        ))
    }

    assertResult(Some(sources.StringContains("cstr", "a"))) {
      DataSourceStrategy.translateFilter(
        expressions.Contains(attrStr, Literal("a")
        ))
    }
  }

  test("translate complex expression") {
    val attrInt = AttributeReference("cint", IntegerType)()

    assertResult(None) {
      DataSourceStrategy.translateFilter(
        expressions.LessThanOrEqual(
          expressions.Subtract(expressions.Abs(attrInt), Literal(2)), Literal(1)))
    }

    assertResult(Some(sources.Or(
      sources.And(
        sources.GreaterThan("cint", 1),
        sources.LessThan("cint", 10)),
      sources.And(
        sources.GreaterThan("cint", 50),
        sources.LessThan("cint", 100))))) {
      DataSourceStrategy.translateFilter(expressions.Or(
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(attrInt, Literal(10))
        ),
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(50)),
          expressions.LessThan(attrInt, Literal(100))
        )
      ))
    }
    // SPARK-22548 Incorrect nested AND expression pushed down to JDBC data source
    assertResult(None) {
      DataSourceStrategy.translateFilter(expressions.Or(
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(
            expressions.Abs(attrInt),
            Literal(10))
        ),
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(50)),
          expressions.LessThan(attrInt, Literal(100))
        )
      ))
    }
    assertResult(None) {
      DataSourceStrategy.translateFilter(
        expressions.Not(expressions.And(
          expressions.Or(
            expressions.LessThanOrEqual(attrInt, Literal(1)),
            expressions.GreaterThanOrEqual(
              expressions.Abs(attrInt),
              Literal(10))
          ),
          expressions.Or(
            expressions.LessThanOrEqual(attrInt, Literal(50)),
            expressions.GreaterThanOrEqual(attrInt, Literal(100))
          )
        )))
    }

    assertResult(Some(sources.Or(
      sources.Or(
        sources.EqualTo("cint", 1),
        sources.EqualTo("cint", 10)),
      sources.Or(
        sources.GreaterThan("cint", 0),
        sources.LessThan("cint", -10))))) {
      DataSourceStrategy.translateFilter(expressions.Or(
        expressions.Or(
          expressions.EqualTo(attrInt, Literal(1)),
          expressions.EqualTo(attrInt, Literal(10))
        ),
        expressions.Or(
          expressions.GreaterThan(attrInt, Literal(0)),
          expressions.LessThan(attrInt, Literal(-10))
        )
      ))
    }
    assertResult(None) {
      DataSourceStrategy.translateFilter(expressions.Or(
        expressions.Or(
          expressions.EqualTo(attrInt, Literal(1)),
          expressions.EqualTo(
            expressions.Abs(attrInt),
            Literal(10))
        ),
        expressions.Or(
          expressions.GreaterThan(attrInt, Literal(0)),
          expressions.LessThan(attrInt, Literal(-10))
        )
      ))
    }

    assertResult(Some(sources.And(
      sources.And(
        sources.GreaterThan("cint", 1),
        sources.LessThan("cint", 10)),
      sources.And(
        sources.EqualTo("cint", 6),
        sources.IsNotNull("cint"))))) {
      DataSourceStrategy.translateFilter(expressions.And(
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(attrInt, Literal(10))
        ),
        expressions.And(
          expressions.EqualTo(attrInt, Literal(6)),
          expressions.IsNotNull(attrInt)
        )
      ))
    }
    assertResult(None) {
      DataSourceStrategy.translateFilter(expressions.And(
        expressions.And(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(attrInt, Literal(10))
        ),
        expressions.And(
          expressions.EqualTo(expressions.Abs(attrInt),
            Literal(6)),
          expressions.IsNotNull(attrInt)
        )
      ))
    }

    assertResult(Some(sources.And(
      sources.Or(
        sources.GreaterThan("cint", 1),
        sources.LessThan("cint", 10)),
      sources.Or(
        sources.EqualTo("cint", 6),
        sources.IsNotNull("cint"))))) {
      DataSourceStrategy.translateFilter(expressions.And(
        expressions.Or(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(attrInt, Literal(10))
        ),
        expressions.Or(
          expressions.EqualTo(attrInt, Literal(6)),
          expressions.IsNotNull(attrInt)
        )
      ))
    }
    assertResult(None) {
      DataSourceStrategy.translateFilter(expressions.And(
        expressions.Or(
          expressions.GreaterThan(attrInt, Literal(1)),
          expressions.LessThan(attrInt, Literal(10))
        ),
        expressions.Or(
          expressions.EqualTo(expressions.Abs(attrInt),
            Literal(6)),
          expressions.IsNotNull(attrInt)
        )
      ))
    }
  }
}
