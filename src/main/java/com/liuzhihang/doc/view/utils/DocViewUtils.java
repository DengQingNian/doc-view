package com.liuzhihang.doc.view.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.constant.SwaggerConstant;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * DocView 通用处理类
 * <p>
 * 从 settings 中获取配置,以及用户自定义的配置进行判断 统一返回
 *
 * @author liuzhihang
 * @date 2021/6/10 20:10
 */
public class DocViewUtils {

    private DocViewUtils() {
    }

    /**
     * 匿名类和内部类可能返回 null
     * <p>
     * 文档标题(接口分类):
     * 从类/类注释中获取标题
     *
     * @param psiClass
     * @return
     */
    @NotNull
    public static String getTitle(@NotNull PsiClass psiClass) {

        Settings settings = Settings.getInstance(psiClass.getProject());

        if (settings.getTitleUseCommentTag()) {
            // 注释 @DocView.Title
            String docTitleTagValue = CustomPsiCommentUtils.getComment(psiClass.getDocComment(), settings.getTitleTag());

            if (StringUtils.isNotBlank(docTitleTagValue)) {
                return docTitleTagValue;
            }
        }

        if (settings.getTitleUseFullClassName()) {
            // 获取全类名
            String fullClassName = psiClass.getQualifiedName();

            if (StringUtils.isNotBlank(fullClassName)) {
                return fullClassName;
            }
        }

        if (settings.getTitleUseSimpleClassName()) {
            String className = psiClass.getName();
            if (StringUtils.isNotBlank(className)) {
                return className;
            }
        }

        return "";
    }

    /**
     * 获取方法名字:
     * <p>
     * 方法名字(接口标题):
     * <p>
     * 支持 Swagger/方法名/自定义注释 tag
     *
     * @param psiMethod
     * @return
     */
    @NotNull
    public static String getName(@NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(psiMethod.getProject());
        // swagger v3 @Operation
        if (settings.getNameUseSwagger3()) {
            PsiAnnotation tagAnnotation = psiMethod.getAnnotation(SwaggerConstant.OPERATION);
            if (tagAnnotation != null) {
                PsiAnnotationMemberValue value = tagAnnotation.findAttributeValue("name");
                if (value != null) {
                    return value.getText();
                }
            }
        }
        // swagger @ApiOperation
        if (settings.getNameUseSwagger()) {
            PsiAnnotation apiAnnotation = psiMethod.getAnnotation(SwaggerConstant.API_OPERATION);
            if (apiAnnotation != null) {
                PsiAnnotationMemberValue value = apiAnnotation.findAttributeValue("value");
                if (value != null) {
                    return value.getText();
                }
            }
        }

        // 注释上的 tag
        if (settings.getNameUseCommentTag()) {
            String comment = CustomPsiCommentUtils.getComment(psiMethod.getDocComment(), settings.getNameTag());

            if (StringUtils.isNotBlank(comment)) {
                return comment;
            }
        }
        return psiMethod.getName();
    }

    /**
     * 获取方法描述, 方法描述直接获取注释
     * <p>
     * 可从 Settings 中按照配置获取
     *
     * @param psiMethod
     * @return
     */
    @NotNull
    public static String getMethodDesc(@NotNull PsiMethod psiMethod) {

        Settings settings = Settings.getInstance(psiMethod.getProject());

        // 从 swagger3 中获取描述
        if (settings.getDescUseSwagger3()) {
            PsiAnnotation operationAnnotation = psiMethod.getAnnotation(SwaggerConstant.OPERATION);
            if (operationAnnotation != null) {
                PsiAnnotationMemberValue value = operationAnnotation.findAttributeValue("description");
                if (value != null) {
                    return value.getText();
                }
            }
        }
        // 先从 swagger 中获取描述
        if (settings.getDescUseSwagger()) {
            PsiAnnotation apiOperationAnnotation = psiMethod.getAnnotation(SwaggerConstant.API_OPERATION);
            if (apiOperationAnnotation != null) {
                PsiAnnotationMemberValue value = apiOperationAnnotation.findAttributeValue("notes");
                if (value != null) {
                    return value.getText();
                }
            }
        }
        // 最后从注释中获取

        return CustomPsiCommentUtils.getComment(psiMethod.getDocComment());
    }


    /**
     * 判断是否是需要排除的字段
     *
     * @param psiField
     * @return
     */
    @NotNull
    public static boolean isExcludeField(@NotNull PsiField psiField) {

        Settings settings = Settings.getInstance(psiField.getProject());

        if (settings.getExcludeFieldNames().contains(psiField.getName())) {
            return true;
        }
        // 排除掉被 static 修饰的字段
        if (CustomPsiUtils.hasModifierProperty(psiField, PsiModifier.STATIC)) {
            return true;
        }

        // 排除部分注解的字段
        if (AnnotationUtil.isAnnotated(psiField, settings.getExcludeFieldAnnotation(), 0)) {
            return true;
        }

        return false;
    }


    /**
     * 判断字段是否必填
     *
     * @param psiField
     * @return
     */
    public static boolean isRequired(@NotNull PsiField psiField) {

        Settings settings = Settings.getInstance(psiField.getProject());

        if (AnnotationUtil.isAnnotated(psiField, settings.getRequiredFieldAnnotation(), 0)) {
            return true;
        }


        // swagger v3 @Schema
        PsiAnnotation schemaAnnotation = psiField.getAnnotation(SwaggerConstant.SCHEMA);
        if (schemaAnnotation != null) {
            PsiAnnotationMemberValue value = schemaAnnotation.findAttributeValue("required");
            if (value != null && value.getText() != null && value.getText().contains("true")) {
                return true;
            }
        }
        // swagger @ApiModelProperty
        PsiAnnotation apiModelPropertyAnnotation = psiField.getAnnotation(SwaggerConstant.API_MODEL_PROPERTY);
        if (apiModelPropertyAnnotation != null) {
            PsiAnnotationMemberValue value = apiModelPropertyAnnotation.findAttributeValue("required");
            if (value != null && value.getText() != null && value.getText().contains("true")) {
                return true;
            }
        }

        if (settings.getRequiredUseCommentTag()) {
            // 查看注释
            PsiDocComment docComment = psiField.getDocComment();

            if (docComment == null) {
                return true;
            }

            PsiDocTag requiredTag = docComment.findTagByName(settings.getRequired());

            if (requiredTag != null) {
                return true;
            }

        }

        return false;

    }

    public static boolean isRequired(@NotNull PsiParameter psiParameter) {

        Settings settings = Settings.getInstance(psiParameter.getProject());

        return AnnotationUtil.isAnnotated(psiParameter, settings.getRequiredFieldAnnotation(), 0);
    }


    /**
     * 获取字段的描述
     *
     * @param psiField
     * @return
     */
    @NotNull
    public static String fieldDesc(@NotNull PsiField psiField) {

        PsiDocComment docComment = psiField.getDocComment();

        if (docComment != null) {
            // param.setExample();
            // 参数举例, 使用 tag 判断
            return CustomPsiCommentUtils.getComment(docComment);
        }
        return "";
    }
}
