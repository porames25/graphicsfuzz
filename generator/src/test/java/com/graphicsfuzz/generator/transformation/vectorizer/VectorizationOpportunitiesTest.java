/*
 * Copyright 2018 The GraphicsFuzz Project Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.graphicsfuzz.generator.transformation.vectorizer;

import com.graphicsfuzz.common.ast.TranslationUnit;
import com.graphicsfuzz.common.glslversion.ShadingLanguageVersion;
import com.graphicsfuzz.common.util.CannedRandom;
import com.graphicsfuzz.common.util.ParseHelper;
import com.graphicsfuzz.common.util.ParseTimeoutException;
import com.graphicsfuzz.generator.util.TestingHelpers;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VectorizationOpportunitiesTest {

  @Test
  public void testVectorizationOpportunities() throws IOException, ParseTimeoutException {
    final String program =
            "vec3 f()\n"
            + "{\n"
            + "    float x;"
            + "    float a, b, c, d, e = a = b = c = d = 0.;\n"
            + "    vec2 g = vec2(1., 0.);\n"
            + "    return col;\n"
            + "}\n";

    final String expected =
        "vec3 f()\n"
            + "{\n"
            + "    vec4 GLF_merged3_0_1_1_1_2_1_3_1_1agx;\n"
            + "    float x;\n"
            + "    GLF_merged3_0_1_1_1_2_1_3_1_1agx.w = x;\n"
            + "    float a, b, c, d, e = a = b = c = d = 0.;\n"
            + "    GLF_merged3_0_1_1_1_2_1_3_1_1agx.x = a;\n"
            + "    vec2 g = vec2(1., 0.);\n"
            + "    GLF_merged3_0_1_1_1_2_1_3_1_1agx.yz = g;\n"
            + "    return col;\n"
            + "}";

    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
        new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
            new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(1, ops.size());
    ops.get(0).apply();
    TestingHelpers.assertShadersMatch(expected, tu);
  }

  @Test
  public void testMutualReferences() throws Exception {
    final String program =
        "vec3 f()\n"
            + "{\n"
            + "    vec3 l = vec3(0.0);"
            + "    l = normalize(l);"
            + "    float r = dot(l, l);"
            + "}\n";

    final String expected =
        "vec3 f()\n"
            + "{\n"
            + "    vec4 GLF_merged2_0_3_1_3_1_1lr;\n"
            + "    vec3 l = vec3(0.0);\n"
            + "    GLF_merged2_0_3_1_3_1_1lr.xyz = l;\n"
            + "    GLF_merged2_0_3_1_3_1_1lr.xyz = normalize(GLF_merged2_0_3_1_3_1_1lr.xyz);\n"
            + "    float r = dot(GLF_merged2_0_3_1_3_1_1lr.xyz, GLF_merged2_0_3_1_3_1_1lr.xyz);\n"
            + "    GLF_merged2_0_3_1_3_1_1lr.w = r;\n"
            + "}";

    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
        new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
            new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(1, ops.size());
    ops.get(0).apply();
    TestingHelpers.assertShadersMatch(expected, tu);

  }

  @Test
  public void testSwitch() throws Exception {
    final String program =
        "vec3 f(int x)\n"
            + "{\n"
            + "    switch(x) {"
            + "      case 0:"
            + "      vec3 l = vec3(0.0);"
            + "      case 1:"
            + "      l = normalize(l);"
            + "      default:"
            + "      float r = dot(l, l);"
            + "    }"
            + "}\n";

    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
        new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
            new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(0, ops.size());
  }

  @Test
  public void testSwitch2() throws Exception {
    final String program =
        "vec3 f(int x)\n"
            + "{\n"
            + "    switch(x) {"
            + "      case 0:"
            + "      { vec3 l = vec3(0.0);"
            + "        l = normalize(l);"
            + "        float r = dot(l, l);"
            + "      }"
            + "      default: break;"
            + "    }"
            + "}\n";

    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
        new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
            new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(1, ops.size());
  }

  @Test
  public void testLoops() throws Exception {
    final String program = "void main() { for(int i = 0; i < 10; i++) { } for (int j = 0; j < 10; j++) { } }";
    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
          new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
                new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(0, ops.size());

  }

  @Test
  public void testVectorizationOpportunitiesNested() throws IOException, ParseTimeoutException {
    final String program =
          "float f()\n"
                + "{\n"
                + "    float a = 1;"
                + "    float b = 2;"
                + "    float c = 3;"
                + "    return a;\n"
                + "}\n";

    final String expectedFirst =
          "float f()\n"
                + "{\n"
                + "    vec3 GLF_merged3_0_1_1_1_1_1_2_1_1abc;\n"
                + "    float a = 1;\n"
                + "    GLF_merged3_0_1_1_1_1_1_2_1_1abc.x = a;\n"
                + "    float b = 2;\n"
                + "    GLF_merged3_0_1_1_1_1_1_2_1_1abc.y = b;\n"
                + "    float c = 3;\n"
                + "    GLF_merged3_0_1_1_1_1_1_2_1_1abc.z = c;\n"
                + "    return GLF_merged3_0_1_1_1_1_1_2_1_1abc.x;\n"
                + "}\n";

    final String expectedSecond =
          "float f()\n"
                + "{\n"
                + "    vec4 GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca;\n"
                + "    vec3 GLF_merged3_0_1_1_1_1_1_2_1_1abc;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz = GLF_merged3_0_1_1_1_1_1_2_1_1abc;\n"
                + "    float a = 1;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.w = a;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.x = GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.w;\n"
                + "    float b = 2;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.y = b;\n"
                + "    float c = 3;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.z = c;\n"
                + "    return GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.x;\n"
                + "}\n";

    final String expectedThird =
          "float f()\n"
                + "{\n"
                + "    vec2 GLF_merged2_0_1_1_1_1_1bc;\n"
                + "    vec4 GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca;\n"
                + "    vec3 GLF_merged3_0_1_1_1_1_1_2_1_1abc;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz = GLF_merged3_0_1_1_1_1_1_2_1_1abc;\n"
                + "    float a = 1;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.w = a;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.x = GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.w;\n"
                + "    float b = 2;\n"
                + "    GLF_merged2_0_1_1_1_1_1bc.x = b;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.y = GLF_merged2_0_1_1_1_1_1bc.x;\n"
                + "    float c = 3;\n"
                + "    GLF_merged2_0_1_1_1_1_1bc.y = c;\n"
                + "    GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.z = GLF_merged2_0_1_1_1_1_1bc.y;\n"
                + "    return GLF_merged2_0_3_32_3_1_1GLF_merged3_0_1_1_1_1_1_2_1_1abca.xyz.x;\n"
                + "}\n";

    TranslationUnit tu = ParseHelper.parse(program);
    List<VectorizationOpportunity> ops =
          new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
                new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(1, ops.size());
    ops.get(0).apply();
    TestingHelpers.assertShadersMatch(expectedFirst, tu);

    // Now do a nested application
    ops =
          new VectorizationOpportunities(tu, ShadingLanguageVersion.GLSL_440,
                new CannedRandom(0, 0, 0, 0, 0, 0, 0)).getAllOpportunities();
    assertEquals(2, ops.size());
    ops.get(0).apply();
    TestingHelpers.assertShadersMatch(expectedSecond, tu);
    ops.get(1).apply();
    TestingHelpers.assertShadersMatch(expectedThird, tu);

  }

}