// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.healthcare.imaging.dicomadapter;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;

import java.util.Objects;

public class DestinationFilter {

  private static final String AE_TITLE = "AETitle";

  private final String aeTitle;
  private final Attributes filterAttrs;

  public DestinationFilter(String filterString) {
    String aeTitle = null;
    this.filterAttrs = new Attributes();

    if (filterString != null && filterString.length() > 0) {
      String[] params = filterString.split("&");
      for (String param : params) {
        String[] keyValue = param.split("=");
        if (keyValue.length != 2) {
          throw new IllegalArgumentException(
              "Invalid filter parameter: " + param);
        }
        if (keyValue[0].equals(AE_TITLE)) {
          aeTitle = keyValue[1];
        } else {
          int tag = TagUtils.forName(keyValue[0]);
          if (tag == -1) {
            throw new IllegalArgumentException(
                "Invalid tag in filter string: " + keyValue[0]);
          }
          this.filterAttrs.setString(tag, VR.LO,
              keyValue[1]); // VR just needs to be any string type for match()
        }
      }
    }

    this.aeTitle = aeTitle;
  }

  public String getAeTitle() {
    return aeTitle;
  }

  public Attributes getFilterAttrs() {
    return filterAttrs;
  }

  public boolean matches(String incomingAet, Attributes incomingAttrs) {
    return (aeTitle == null || aeTitle.equals(incomingAet)) &&
        (this.filterAttrs.size() == 0
            || incomingAttrs.matches(this.filterAttrs, false, false));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DestinationFilter that = (DestinationFilter) o;
    return Objects.equals(aeTitle, that.aeTitle) &&
        filterAttrs.equals(that.filterAttrs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aeTitle, filterAttrs);
  }
}
