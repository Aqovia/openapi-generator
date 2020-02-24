package org.openapitools.codegen.languages;

import com.samskivert.mustache.Mustache;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.openapitools.codegen.*;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.URLPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.UUID.randomUUID;

public class CsharpFunctionsServerCodegen extends AbstractCSharpCodegen {

    public static final String ASPNET_CORE_VERSION = "aspnetCoreVersion";
    public static final String IS_FUNCTIONS_V2 = "isFunctionsV2";
    public static final String ALLOW_IMPLEMENTATION_IN_SEPARATE_METHOD = "allowImplementationInSeparateMethod";
    public static final String CLASS_MODIFIER = "classModifier";
    public static final String OPERATION_IS_ASYNC = "operationIsAsync";
    public static final String OPERATION_RESULT_TASK = "operationResultTask";
    public static final String MODEL_CLASS_MODIFIER = "modelClassModifier";

    private String packageGuid = "{" + randomUUID().toString().toUpperCase(Locale.ROOT) + "}";

    @SuppressWarnings("hiding")
    protected Logger LOGGER = LoggerFactory.getLogger(CsharpFunctionsServerCodegen.class);

    protected int serverPort = 8080;
    protected String serverHost = "0.0.0.0";
    ; // default to 3.1
    protected CliOption aspnetCoreVersion = new CliOption(ASPNET_CORE_VERSION, "3.1, 2.2, 2.1");
    protected boolean isFunctionsV2 = false;
    private boolean allowImplementationInSeparateMethod = false;
    private CliOption classModifier = new CliOption(CLASS_MODIFIER, "Class Modifier can be empty or partial");
    private CliOption modelClassModifier = new CliOption(MODEL_CLASS_MODIFIER, "Model Class Modifier can be empty or partial");
    private boolean operationIsAsync = true;
    private boolean operationResultTask = true;

    public CsharpFunctionsServerCodegen() {
        super();

        outputFolder = "generated-code" + File.separator + getName();

        modelTemplateFiles.put("model.mustache", ".cs");
        apiTemplateFiles.put("function.mustache", ".cs");

        embeddedTemplateDir = templateDir = "csharp-functions-server";

        // contextually reserved words
        // NOTE: C# uses camel cased reserved words, while models are title cased. We don't want lowercase comparisons.
        reservedWords.addAll(
                Arrays.asList("var", "async", "await", "dynamic", "yield")
        );

        cliOptions.clear();

        typeMapping.put("boolean", "bool");
        typeMapping.put("integer", "int");
        typeMapping.put("float", "float");
        typeMapping.put("long", "long");
        typeMapping.put("double", "double");
        typeMapping.put("number", "decimal");
        typeMapping.put("DateTime", "DateTime");
        typeMapping.put("date", "DateTime");
        typeMapping.put("UUID", "Guid");
        typeMapping.put("URI", "string");

        setSupportNullable(Boolean.TRUE);

        // CLI options
        addOption(CodegenConstants.LICENSE_URL,
                CodegenConstants.LICENSE_URL_DESC,
                licenseUrl);

        addOption(CodegenConstants.LICENSE_NAME,
                CodegenConstants.LICENSE_NAME_DESC,
                licenseName);

        addOption(CodegenConstants.PACKAGE_COPYRIGHT,
                CodegenConstants.PACKAGE_COPYRIGHT_DESC,
                packageCopyright);

        addOption(CodegenConstants.PACKAGE_AUTHORS,
                CodegenConstants.PACKAGE_AUTHORS_DESC,
                packageAuthors);

        addOption(CodegenConstants.PACKAGE_TITLE,
                CodegenConstants.PACKAGE_TITLE_DESC,
                packageTitle);

        addOption(CodegenConstants.PACKAGE_NAME,
                "C# package name (convention: Title.Case).",
                packageName);

        addOption(CodegenConstants.PACKAGE_VERSION,
                "C# package version.",
                packageVersion);

        addOption(CodegenConstants.OPTIONAL_PROJECT_GUID,
                CodegenConstants.OPTIONAL_PROJECT_GUID_DESC,
                null);

        addOption(CodegenConstants.SOURCE_FOLDER,
                CodegenConstants.SOURCE_FOLDER_DESC,
                sourceFolder);

        aspnetCoreVersion.addEnum("2.1", "ASP.NET Core 2.1");
        aspnetCoreVersion.addEnum("2.2", "ASP.NET Core 2.2");
        aspnetCoreVersion.addEnum("3.1", "ASP.NET Core 3.1");
        aspnetCoreVersion.setDefault("3.1");
        aspnetCoreVersion.setOptValue(aspnetCoreVersion.getDefault());
        addOption(aspnetCoreVersion.getOpt(), aspnetCoreVersion.getDescription(), aspnetCoreVersion.getOptValue());

        // CLI Switches
        addSwitch(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG,
                CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG_DESC,
                sortParamsByRequiredFlag);

        addSwitch(CodegenConstants.USE_DATETIME_OFFSET,
                CodegenConstants.USE_DATETIME_OFFSET_DESC,
                useDateTimeOffsetFlag);

        addSwitch(CodegenConstants.USE_COLLECTION,
                CodegenConstants.USE_COLLECTION_DESC,
                useCollection);

        addSwitch(CodegenConstants.RETURN_ICOLLECTION,
                CodegenConstants.RETURN_ICOLLECTION_DESC,
                returnICollection);

        addOption(CodegenConstants.ENUM_NAME_SUFFIX,
                CodegenConstants.ENUM_NAME_SUFFIX_DESC,
                enumNameSuffix);

        addOption(CodegenConstants.ENUM_VALUE_SUFFIX,
                "Suffix that will be appended to all enum values.",
                enumValueSuffix);

        classModifier.addEnum("", "Keep class default with no modifier");
        classModifier.addEnum("partial", "Make class partial");
        classModifier.setDefault("");
        classModifier.setOptValue(classModifier.getDefault());
        addOption(classModifier.getOpt(), classModifier.getDescription(), classModifier.getOptValue());

        addSwitch(OPERATION_IS_ASYNC,
                "Set methods to async (default) or sync.",
                operationIsAsync);

        addSwitch(OPERATION_RESULT_TASK,
                "Set methods' result to Task<>.",
                operationResultTask);

        addSwitch(ALLOW_IMPLEMENTATION_IN_SEPARATE_METHOD,
                "Construct Function bodies that allow for an actual implementation to be provided in a separate method",
                allowImplementationInSeparateMethod);

        modelClassModifier.setType("String");
        modelClassModifier.addEnum("", "Keep model class default with no modifier");
        modelClassModifier.addEnum("partial", "Make model class partial");
        modelClassModifier.setDefault("partial");
        modelClassModifier.setOptValue(modelClassModifier.getDefault());
        addOption(modelClassModifier.getOpt(), modelClassModifier.getDescription(), modelClassModifier.getOptValue());
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "csharp-functions-server";
    }

    @Override
    public String getHelp() {
        return "Generates a C# Azure Functions API server.";
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        URL url = URLPathUtils.getServerURL(openAPI, serverVariableOverrides());
        additionalProperties.put("serverHost", url.getHost());
        additionalProperties.put("serverPort", URLPathUtils.getPort(url, 8080));

        setApiBasePath();
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.OPTIONAL_PROJECT_GUID)) {
            setPackageGuid((String) additionalProperties.get(CodegenConstants.OPTIONAL_PROJECT_GUID));
        }
        additionalProperties.put("packageGuid", packageGuid);

        // Check for the modifiers etc.
        // The order of the checks is important.
        setClassModifier();
        setModelClassModifier();
        setOperationResultTask();
        setOperationIsAsync();
        setAllowImplementationInSeparateMethod();

        additionalProperties.put("dockerTag", packageName.toLowerCase(Locale.ROOT));

        if (!additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            apiPackage = packageName + ".Functions";
            additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            modelPackage = packageName + ".Models";
            additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        }

        String packageFolder = sourceFolder + File.separator + packageName;

        // determine the ASP.NET core version setting
        setAspnetCoreVersion(packageFolder);

        supportingFiles.add(new SupportingFile("gitignore", packageFolder, ".gitignore"));
        supportingFiles.add(new SupportingFile("Project.csproj.mustache", packageFolder, packageName + ".csproj"));
        supportingFiles.add(new SupportingFile("host.json", packageFolder, "host.json"));
        supportingFiles.add(new SupportingFile("local.settings.json", packageFolder, "local.settings.json"));
        supportingFiles.add(new SupportingFile("typeConverter.mustache", packageFolder + File.separator + "Converters", "CustomEnumConverter.cs"));
    }

    public void setPackageGuid(String packageGuid) {
        this.packageGuid = packageGuid;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + packageName + File.separator + "Functions";
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + packageName + File.separator + "Models";
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        generateJSONSpecFile(objs);
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    protected void processOperation(CodegenOperation operation) {
        super.processOperation(operation);

        // HACK: Unlikely in the wild, but we need to clean operation paths for MVC Routing
        if (operation.path != null) {
            String original = operation.path;
            operation.path = operation.path.replace("?", "/");
            if (!original.equals(operation.path)) {
                LOGGER.warn("Normalized " + original + " to " + operation.path + ". Please verify generated source.");
            }
        }

        // Remove leading '/' from path
        if (operation.path != null && operation.path.startsWith("/"))
           operation.path = operation.path.substring(1);
    }

    @Override
    public Mustache.Compiler processCompiler(Mustache.Compiler compiler) {
        // To avoid unexpected behaviors when options are passed programmatically such as { "useCollection": "" }
        return super.processCompiler(compiler).emptyStringIsFalse(true);
    }

    @Override
    public String toRegularExpression(String pattern) {
        return escapeText(pattern);
    }

    @Override
    public String getNullableType(Schema p, String type) {
        if (languageSpecificPrimitives.contains(type)) {
            if (isSupportNullable() && ModelUtils.isNullable(p) && nullableType.contains(type)) {
                return type + "?";
            } else {
                return type;
            }
        } else {
            return null;
        }
    }

    private void setCliOption(CliOption cliOption) throws IllegalArgumentException {
        if (additionalProperties.containsKey(cliOption.getOpt())) {
            // TODO Hack - not sure why the empty strings become boolean.
            Object obj = additionalProperties.get(cliOption.getOpt());
            if (!SchemaTypeUtil.BOOLEAN_TYPE.equals(cliOption.getType())) {
                if (obj instanceof Boolean) {
                    obj = "";
                    additionalProperties.put(cliOption.getOpt(), obj);
                }
            }
            cliOption.setOptValue(obj.toString());
        } else {
            additionalProperties.put(cliOption.getOpt(), cliOption.getOptValue());
        }
        if (cliOption.getOptValue() == null) {
            cliOption.setOptValue(cliOption.getDefault());
            throw new IllegalArgumentException(cliOption.getOpt() + ": Invalid value '" + additionalProperties.get(cliOption.getOpt()).toString() + "'" +
                    ". " + cliOption.getDescription());
        }
    }

    private void setClassModifier() {
        // Check for class modifier if not present set the default value.
        setCliOption(classModifier);
    }

    private void setModelClassModifier() {
        setCliOption(modelClassModifier);
    }
    
    private void setAspnetCoreVersion(String packageFolder) {
        setCliOption(aspnetCoreVersion);

        isFunctionsV2 = aspnetCoreVersion.getOptValue().startsWith("2.");
        additionalProperties.put(IS_FUNCTIONS_V2, isFunctionsV2);
        
        LOGGER.info("ASP.NET core version: " + aspnetCoreVersion.getOptValue());
    }
    
    private void setOperationResultTask() {
        if (additionalProperties.containsKey(OPERATION_RESULT_TASK)) {
            operationResultTask = convertPropertyToBooleanAndWriteBack(OPERATION_RESULT_TASK);
        } else {
            additionalProperties.put(OPERATION_RESULT_TASK, operationResultTask);
        }
    }
    
    private void setOperationIsAsync() {
        if (additionalProperties.containsKey(OPERATION_IS_ASYNC)) {
            operationIsAsync = convertPropertyToBooleanAndWriteBack(OPERATION_IS_ASYNC);
        } else {
            additionalProperties.put(OPERATION_IS_ASYNC, operationIsAsync);
        }
    }

    private void setAllowImplementationInSeparateMethod() {
        if (additionalProperties.containsKey(ALLOW_IMPLEMENTATION_IN_SEPARATE_METHOD)) {
            allowImplementationInSeparateMethod = convertPropertyToBooleanAndWriteBack(ALLOW_IMPLEMENTATION_IN_SEPARATE_METHOD);
        } else {
            additionalProperties.put(ALLOW_IMPLEMENTATION_IN_SEPARATE_METHOD, allowImplementationInSeparateMethod);
        }
        
        // force partial class if true
        if (allowImplementationInSeparateMethod)
            additionalProperties.put(CLASS_MODIFIER, "partial");
    }

    private void setApiBasePath() {
        URL url = URLPathUtils.getServerURL(openAPI, serverVariableOverrides());
        String apiBasePath = encodePath(url.getPath()).replaceAll("/$", "");
        
        // if a base path exists, remove leading '/' and append trailing '/'
        if (apiBasePath != null && apiBasePath.length() > 0) {
            if (apiBasePath.startsWith("/"))
                apiBasePath = apiBasePath.substring(1);
            
            if (!apiBasePath.endsWith("/"))
                apiBasePath += "/";
        }

        additionalProperties.put("apiBasePath", apiBasePath);
    }
}