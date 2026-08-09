package com.myblog.starter.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MyBlog 后端 API",
                version = "1.0.0",
                description = "MyBlog 博客前台、后台管理与版本化内容发布 API。",
                contact = @Contact(name = "MyBlog 管理员")
        ),
        servers = @Server(url = "/", description = "当前部署环境"),
        tags = {
                @Tag(name = "健康检查", description = "服务健康状态"),
                @Tag(name = "认证", description = "登录、令牌、当前用户与密码管理"),
                @Tag(name = "公开内容", description = "博客前台读取当前已发布内容"),
                @Tag(name = "内容管理", description = "模块草稿、发布、下线、历史与恢复"),
                @Tag(name = "MyLab 标签", description = "不参与版本管理的全局标签"),
                @Tag(name = "文件管理", description = "图片和 Markdown 资源管理"),
                @Tag(name = "管理员", description = "后台管理员账号管理"),
                @Tag(name = "系统", description = "系统静态与运行状态")
        }
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "登录后填写 access_token"
)
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";
    private static final String ERROR_SCHEMA = "ApiErrorResponse";

    @Bean
    public OpenAPI myBlogOpenApi() {
        Schema<?> errorSchema = new ObjectSchema()
                .description("统一错误响应")
                .addProperty("code", new IntegerSchema().description("业务错误码").example(10007))
                .addProperty("message", new StringSchema().description("错误说明").example("请求参数校验失败"))
                .addProperty("data", new ObjectSchema().nullable(true))
                .addProperty("error", new StringSchema().description("可安全展示的错误细节").nullable(true));
        errorSchema.setRequired(List.of("code", "message"));

        Components components = new Components()
                .addSchemas(ERROR_SCHEMA, errorSchema)
                .addSchemas("SkillsContent", collectionSchema("items",
                        "id", "skill_key", "name", "percentage", "level_code", "level_text", "icon", "bar_style", "is_new"))
                .addSchemas("FootprintsContent", collectionSchema("items",
                        "id", "city_key", "title", "summary", "contents", "images"))
                .addSchemas("HobbiesContent", collectionSchema("items",
                        "id", "hobby_key", "title", "description", "image"))
                .addSchemas("VibeContent", collectionSchema("items",
                        "id", "tool_key", "name", "percentage", "description"))
                .addSchemas("MyLabContent", new ObjectSchema()
                        .addProperty("tags", new ArraySchema().items(itemSchema("id", "tag_key", "name")))
                        .addProperty("cards", new ArraySchema().items(itemSchema(
                                "id", "post_key", "card_title", "card_summary", "post_date", "tags",
                                "card_type", "project_show_order", "project_contents", "image_resource_id",
                                "content_resource_id", "markdown_url"))));
        return new OpenAPI().components(components);
    }

    private Schema<?> collectionSchema(String field, String... itemFields) {
        return new ObjectSchema().addProperty(field, new ArraySchema().items(itemSchema(itemFields)));
    }

    private ObjectSchema itemSchema(String... fields) {
        ObjectSchema item = new ObjectSchema();
        for (String field : fields) item.addProperty(field, new Schema<>().description(field));
        return item;
    }

    @Bean
    public OperationCustomizer standardErrorResponses() {
        Map<String, ErrorExample> definitions = new LinkedHashMap<>();
        definitions.put("400", new ErrorExample(10009, "请求体格式错误", "请求格式或参数类型不正确"));
        definitions.put("401", new ErrorExample(10001, "身份认证失败", null));
        definitions.put("403", new ErrorExample(10004, "无权执行该操作", null));
        definitions.put("404", new ErrorExample(10005, "请求的资源不存在", null));
        definitions.put("405", new ErrorExample(10010, "请求方法不支持", null));
        definitions.put("409", new ErrorExample(10006, "资源状态冲突", null));
        definitions.put("413", new ErrorExample(13003, "上传文件超过大小限制", null));
        definitions.put("415", new ErrorExample(10011, "请求媒体类型不支持", null));
        definitions.put("422", new ErrorExample(10007, "请求参数校验失败", "字段不符合约束"));
        definitions.put("429", new ErrorExample(10008, "请求过于频繁", null));
        definitions.put("500", new ErrorExample(20001, "服务器内部错误", null));
        definitions.put("503", new ErrorExample(13005, "文件存储服务不可用", null));

        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            definitions.forEach((status, definition) -> responses.putIfAbsent(status, errorResponse(definition)));
            return operation;
        };
    }

    private ApiResponse errorResponse(ErrorExample definition) {
        io.swagger.v3.oas.models.media.MediaType mediaType = new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))
                .example(example(definition));
        return new ApiResponse().description(definition.message())
                .content(new io.swagger.v3.oas.models.media.Content().addMediaType("application/json", mediaType));
    }

    private Map<String, Object> example(ErrorExample definition) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", definition.code());
        value.put("message", definition.message());
        if (definition.detail() != null) value.put("error", definition.detail());
        return value;
    }

    private record ErrorExample(int code, String message, String detail) {
    }
}
