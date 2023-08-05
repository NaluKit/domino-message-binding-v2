/*
 * Copyright (c) 2018 - 2019 - Frank Hossfeld
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package com.nalukit.domino.v2.message.binding.processor;

import com.github.nalukit.domino.message.binding.client.handling.AbstractMessageDriver;
import com.github.nalukit.domino.message.binding.client.handling.IsMessageDriver;
import com.github.nalukit.domino.message.binding.client.handling.annotation.HasMessageDriverSupport;
import com.github.nalukit.domino.message.binding.client.handling.annotation.MessagePresenter;
import com.github.nalukit.domino.message.binding.client.internal.helper.MessageElementWrapper;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.dominokit.domino.ui.forms.InputFormField;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;

@AutoService(Processor.class)
public class DominoV2MessageProcessor
    extends AbstractProcessor {

  private final static String IMPL_NAME = "MessageDriverImpl";

  private DominoV2MessageProcessorUtils dominoV2MessageProcessorUtils;

  private Map<Element, List<VariableElement>> messagePresenterAnnotatedElements;

  public DominoV2MessageProcessor() {
    super();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return of(HasMessageDriverSupport.class.getCanonicalName()).collect(toSet());
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.setUp();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
                         RoundEnvironment roundEnv) {
    try {
      if (!roundEnv.processingOver()) {
        if (annotations.size() > 0) {
          for (TypeElement annotation : annotations) {
            if (HasMessageDriverSupport.class.getCanonicalName()
                                             .equals(annotation.toString())) {
              handleHasMessageDriverSupportAnnotation(roundEnv);
              for (Element k : this.messagePresenterAnnotatedElements.keySet()) {
                this.generateDriver(k,
                                    this.messagePresenterAnnotatedElements.get(k));
              }
            }
          }
        }
      }
    } catch (DominoV2MessageProcessorException e) {
      this.dominoV2MessageProcessorUtils.createErrorMessage(e.getMessage());
      return true;
    }
    return true;
  }

  private void generateDriver(Element annotatedElement,
                              List<VariableElement> variableElements)
      throws DominoV2MessageProcessorException {
    TypeSpec.Builder typeSpec = TypeSpec.classBuilder(annotatedElement.getSimpleName() + DominoV2MessageProcessor.IMPL_NAME)
                                        .superclass(ParameterizedTypeName.get(ClassName.get(AbstractMessageDriver.class),
                                                                              ClassName.get((TypeElement) annotatedElement)))
                                        .addModifiers(Modifier.PUBLIC,
                                                      Modifier.FINAL)
                                        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IsMessageDriver.class),
                                                                                     ClassName.get((TypeElement) annotatedElement)));

    MethodSpec constructor = MethodSpec.constructorBuilder()
                                       .addModifiers(Modifier.PUBLIC)
                                       .addStatement("super()")
                                       .build();
    typeSpec.addMethod(constructor);

    List<String> usedFieldIds = new ArrayList<>();
    MethodSpec.Builder initializeMethod = MethodSpec.methodBuilder("initialize")
                                                    .addAnnotation(ClassName.get(Override.class))
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(ClassName.get((TypeElement) annotatedElement),
                                                                  "provider");
    HasMessageDriverSupport hasMessageDriverSupportAnnotation = annotatedElement.getAnnotation(HasMessageDriverSupport.class);
    initializeMethod.addStatement("super.clearOnBlur = $L",
                                  hasMessageDriverSupportAnnotation.clearOnBlur());
    for (VariableElement variableElement : variableElements) {
      String messagePresenterId = variableElement.getAnnotation(MessagePresenter.class)
                                                 .value();
      if (usedFieldIds.contains(messagePresenterId)) {
        throw new DominoV2MessageProcessorException("Nalu-Message-Processor: MessagePresenter-ID >>" +
                                                    messagePresenterId +
                                                    "<< is not unique!");
      }
      usedFieldIds.add(messagePresenterId);
      initializeMethod.addStatement("super.messageElementWrappers.put($S, new $T(provider.$L, $S))",
                                    messagePresenterId,
                                    ClassName.get(MessageElementWrapper.class),
                                    variableElement.getSimpleName(),
                                    variableElement.getAnnotation(MessagePresenter.class)
                                                   .value());
    }
    typeSpec.addMethod(initializeMethod.build());

    JavaFile javaFile = JavaFile.builder(this.getPackageAsString(annotatedElement),
                                         typeSpec.build())
                                .build();
    try {
//      System.out.println(javaFile.toString());
      javaFile.writeTo(this.processingEnv.getFiler());
    } catch (IOException e) {
      throw new DominoV2MessageProcessorException("Nalu-Message-Processor: Unable to write generated file: >>" +
                                                  annotatedElement.getSimpleName() +
                                                  DominoV2MessageProcessor.IMPL_NAME +
                                                  "<< -> exception: " +
                                                  e.getMessage());
    }
  }

  private void handleHasMessageDriverSupportAnnotation(RoundEnvironment roundEnv)
      throws DominoV2MessageProcessorException {
    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(HasMessageDriverSupport.class)) {
      this.validateTypeElement(annotatedElement);
      this.messagePresenterAnnotatedElements.put(annotatedElement,
                                                 new ArrayList<>());
      List<Element> annotatedFields = this.getElemntsFromTypeElementAnnotatedWith((TypeElement) annotatedElement);
      for (Element e : annotatedFields) {
        validateVariableElement(e);
        this.messagePresenterAnnotatedElements.get(annotatedElement)
                                              .add((VariableElement) e);
      }
    }
  }

  private void setUp() {
    this.dominoV2MessageProcessorUtils = DominoV2MessageProcessorUtils.builder()
                                                                      .processingEnvironment(processingEnv)
                                                                      .build();
    this.messagePresenterAnnotatedElements = new HashMap<>();
  }

  private void validateTypeElement(Element annotatedElement)
      throws DominoV2MessageProcessorException {
    if (annotatedElement instanceof TypeElement) {
      TypeElement typeElement = (TypeElement) annotatedElement;
      if (!typeElement.getKind()
                      .isClass()) {
        throw new DominoV2MessageProcessorException("Nalu-Message-Processor: @HasNaluMessageDriverSupport must be used with a class");
      }
    } else {
      throw new DominoV2MessageProcessorException("Nalu-Message-Processor:" +
                                                  "@HasNaluMessageDriverSupport can only be used on a type (class)");
    }
  }

  private void validateVariableElement(Element annotatedElement)
      throws DominoV2MessageProcessorException {
    if (annotatedElement instanceof VariableElement) {
      VariableElement variableElement = (VariableElement) annotatedElement;
      if (!variableElement.getKind()
                          .isField()) {
        throw new DominoV2MessageProcessorException("Nalu-Message-Processor: @MessagePresenter must be used with a field");
      }
      if (!this.dominoV2MessageProcessorUtils.extendsClassOrInterface(super.processingEnv.getTypeUtils(),
                                                                      variableElement.asType(),
                                                                      this.processingEnv.getElementUtils()
                                                                         .getTypeElement(InputFormField.class.getCanonicalName())
                                                                         .asType())) {
        throw new DominoV2MessageProcessorException("Nalu-Message-Processor: " +
                                                    variableElement.getSimpleName()
                                                    .toString() +
                                                    ": @MessageSupport: element must extend BasicFormElement (Domino-UI) super class");
      }
    } else {
      throw new DominoV2MessageProcessorException("Nalu-Message-Processory:" +
                                                  "@Nalu-MessageSupport can only be used on a type (field)");
    }
  }

  private String getPackageAsString(Element type) {
    return this.getPackage(type)
               .getQualifiedName()
               .toString();
  }

  private PackageElement getPackage(Element type) {
    while (type.getKind() != ElementKind.PACKAGE) {
      type = type.getEnclosingElement();
    }
    return (PackageElement) type;
  }

  private <A extends Annotation> List<Element> getElemntsFromTypeElementAnnotatedWith(TypeElement element) {
    return this.processingEnv.getElementUtils()
                             .getAllMembers(element)
                             .stream()
                             .filter(methodElement -> methodElement.getAnnotation(MessagePresenter.class) != null)
                             .collect(Collectors.toList());
  }

}
