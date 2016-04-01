// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.ads.adwords.awalerting.report;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A unmodifiable wrap on ReportEntry to ensure it's read-only.
 */
public class UnmodifiableReportRow extends ReportRow {
  public UnmodifiableReportRow(List<String> values, Map<String, Integer> mapping) {
    super(Collections.unmodifiableList(values), Collections.unmodifiableMap(mapping));
  }
}
