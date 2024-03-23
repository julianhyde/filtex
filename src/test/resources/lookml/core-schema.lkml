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
# Definition of the schema of Core LookML (also known as 'LookML-Lite').
#
schema: core {
  enum_type: boolean {
    values: ["false", "true"]
  }
  enum_type: join_type {
    values: ["left_outer", "full_outer", "inner", "cross"]
  }
  enum_type: relationship_type {
    values: ["many_to_one", "many_to_many", "one_to_many", "one_to_one"]
  }
  enum_type: dimension_field_type {
    values: ["bin", "date", "date_time", "distance",
        "duration", "location", "number", "string", "tier", "time",
        "unquoted", "yesno", "zipcode"]
  }
  enum_type: measure_field_type {
    values: ["average", "average_distinct", "count",
        "count_distinct", "date", "list", "max", "median",
        "median_distinct", "min", "number", "percent_of_previous",
        "percent_of_total", "percentile", "percentile_distinct",
        "running_total", "string", "sum", "sum_distinct", "yesno"]
  }
  object_type: conditionally_filter {
    property: filters {
      type: ref_string_map
    }
    property: unless {
      type: ref_list
    }
  }
  object_type: dimension {
    property: type {
      type: dimension_field_type
    }
    property: sql {
      type: code
    }
    property: label {
      type: string
    }
    property: primary_key {
      type: boolean
    }
    property: tags {
      type: string_list
    }
    property: drill_fields {
      type: ref_list
    }
  }
  object_type: measure {
    property: type {
      type: measure_field_type
    }
    property: sql {
      type: code
    }
    property: label {
      type: string
    }
    property: drill_fields {
      type: ref_list
    }
  }
  object_type: view {
    property: from {
      type: ref
    }
    property: label {
      type: string
    }
    property: sql_table_name {
      type: code
    }
    property: dimension {
      type: named_object
    }
    property: measure {
      type: named_object
    }
    property: drill_fields {
      type: ref_list
    }
  }
  object_type: join {
    property: from {
      type: ref
    }
    property: sql_on {
      type: code
    }
    property: relationship {
      type: relationship_type
    }
  }
  object_type: explore {
    property: from {
      type: ref
    }
    property: view_name {
      type: ref
    }
    property: join {
      type: named_object
    }
    property: conditionally_filter {
      type: object
    }
  }
  object_type: model {
    property: explore {
      type: named_object
    }
    property: view {
      type: named_object
    }
    property: fiscal_month_offset {
      type: numeric
    }
  }
  root_properties: [model]
}
