package lombok.Getter;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
public class GetterProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        // 타겟 annotation class 정의
        Set<String> set = new HashSet<>();
        set.add(Getter.class.getName());

        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        // 타겟 source version 정의 (java 8, 11 ...)
        return SourceVersion.RELEASE_11;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elemenets = roundEnv.getElementsAnnotatedWith(Getter.class);

        List<FieldSpec> fieldSpecList = new ArrayList<>();
        List<MethodSpec> methodSpecList = new ArrayList<>();

        for (Element element : elemenets)
        {
            if (element.getKind() != ElementKind.CLASS)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "CustomGetter have to annotated on class");
            }

            TypeElement typeElement = (TypeElement) element;

            for (Element field : typeElement.getEnclosedElements())
            {
                if (field.getKind() == ElementKind.FIELD)
                {
                    String fieldNm = field.getSimpleName().toString();
                    TypeName fieldTypeName = TypeName.get(field.asType());

                    FieldSpec fieldSpec = FieldSpec.builder(fieldTypeName, fieldNm)
                            .addModifiers(Modifier.PRIVATE)
                            .build();
                    fieldSpecList.add(fieldSpec);

                    String methodNm = String.format("get%s", StringUtils.capitalize(fieldNm));
                    String returnStatement = "return "+fieldNm;
                    MethodSpec methodSpec = MethodSpec.methodBuilder(methodNm)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(fieldTypeName)
                            .addStatement(returnStatement)
                            .build();

                    methodSpecList.add(methodSpec);

                }
            }
            ClassName className = ClassName.get(typeElement);
            String getterClassName = String.format("%sGetter", className.simpleName());


            TypeSpec getterClass = TypeSpec.classBuilder(getterClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addFields(fieldSpecList)
                    .addMethods(methodSpecList)
                    .build();

            try
            {
                JavaFile.builder(className.packageName(), getterClass)
                        .build()
                        .writeTo(processingEnv.getFiler());
            }
            catch (IOException e)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ERROR : " + e);
            }
        }

        return true;
    }
}