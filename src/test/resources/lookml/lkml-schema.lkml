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
# Definition of the schema of a LookML Schema file.
#
# This is the 'meta schema'.
#
schema: schema {
  enum_type: type {
    values: [
      # Numeric value. E.g. 'precision: 10'.
      "number",

      # String value. E.g. 'value_format: "$#.00;($#.00)"'.
      "string",

      # Enumerated value. E.g. 'type: left_outer',
      # 'primary_key: yes'.
      "enum",

      # Code block value.
      # E.g. 'sql_on: orders.customer_id = customer.id ;;'.
      "code",

      # Object value. E.g. conditional_filter:
      #
      #   conditional_filter: {
      #     filters: [f1: "123", f2: "abc"]
      #     unless: [f3]
      #   }
      "object",

      # Named-object value. E.g. dimension:
      #
      #   dimension: order_date {
      #     sql: orders.order_date;;
      #   }
      "named_object",

      # Value that is a reference to an object in this model,
      # e.g. 'from: orders'.
      "ref",

      # Value that is a list of references to an objects in this
      # model, e.g. 'drill_fields: [id, name, city, state]'.
      "ref_list",

      # Value that is a list of strings, e.g. 'tags: ["abc", "wyxz"]'.
      "string_list",

      # Value that is a list of reference-string pairs,
      # e.g. 'filters: [id: "123", customer.id: "789"]'.
      "ref_string_map",

      # Value that is a reference-string pair, e.g. 'id: "123"'.
      #
      # Never occurs as a property, only as an element of a list of
      # type "ref_string_map".
      "ref_string"
    ]
  }
  object_type: enum_type {
    property: values {
      type: string_list
    }
  }
  object_type: object_type {
    property: property {
      type: named_object
    }
  }
  object_type: property {
    property: type {
      # 'type' can be either the name of a built-in type (e.g.
      # 'string') or the name of an enum type defined in the model
      # (e.g. 'boolean', 'relationship_type' in LookML-Lite).
      type: ref
    }
  }
  object_type: schema {
    property: enum_type {
      type: named_object
    }
    property: object_type {
      type: named_object
    }
    property: root_properties {
      type: ref_list
    }
  }
  root_properties: [schema]
}
