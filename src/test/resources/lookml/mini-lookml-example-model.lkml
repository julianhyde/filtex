# Licensed to Julian Hyde under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Example model for the Mini-LookML schema. It contains at least one instance
# of each attribute.
#
model: m {
  explore: e {
    from: my_table
    view_name: v
    conditionally_filter: {
      filters: [f1: "123", f2: "abc"]
      unless: [f3, f4]
    }
    join: v {
      from: v
      relationship: many_to_one
      sql_on: v.id = my_table.id;;
    }
  }
  view: v {
    from: v
    label: "my view"
    sql_table_name: catalog.v;;
    drill_fields: []
    dimension: d {
      label: "my dimension"
      type: string
      primary_key: true
      sql: v.d;;
      tags: []
    }
    measure: m {
      label: "my dimension"
      type: sum
      sql: v.amount;;
    }
  }
  fiscal_month_offset: 3
}
