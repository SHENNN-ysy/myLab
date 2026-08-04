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
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
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
                description = "MyBlog 博客前台、后台管理与内容发布 API。所有业务接口统一返回 ApiResponse 包络。",
                contact = @Contact(name = "MyBlog 管理员")
        ),
        servers = @Server(url = "/", description = "当前部署环境"),
        tags = {
                @Tag(name = "健康检查", description = "服务健康状态"),
                @Tag(name = "认证", description = "登录、令牌、当前用户与密码管理"),
                @Tag(name = "公开内容", description = "博客前台读取已发布内容"),
                @Tag(name = "内容管理", description = "内容模块草稿、发布、下线、历史与回滚"),
                @Tag(name = "文件管理", description = "媒体文件上传、查询、签名地址与删除"),
                @Tag(name = "访问统计", description = "访问记录、统计和日志管理"),
                @Tag(name = "管理员", description = "后台管理员账号管理"),
                @Tag(name = "系统", description = "系统静态与运行状态")
        }
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "登录后填写 access_token；Swagger UI 会自动添加 Authorization: Bearer <token>"
)
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";
    private static final String ERROR_SCHEMA = "ApiErrorResponse";

    @Bean
    public OpenAPI myBlogOpenApi() {
        Schema<?> errorSchema = new ObjectSchema()
                .description("统一错误响应")
                .addProperty("code", new IntegerSchema().description("稳定业务错误码").example(10007))
                .addProperty("message", new StringSchema().description("稳定中文错误说明").example("请求参数校验失败"))
                .addProperty("data", new ObjectSchema().description("错误响应固定为 null").nullable(true))
                .addProperty("error", new StringSchema().description("可安全展示的错误细节").nullable(true))
                .addProperty("request_id", new StringSchema().description("请求追踪 ID").example("01J5MYBLOG7P8ABCDEF12345678"))
                .addProperty("timestamp", new IntegerSchema().format("int64")
                        .description("Unix 秒时间戳").example(1785832213L));
        errorSchema.setRequired(List.of("code", "message", "request_id", "timestamp"));
        Components components = new Components()
                .addSchemas(ERROR_SCHEMA, errorSchema)
                .addSchemas("SkillsContent", collectionSchema("items", "技术栈卡片",
                        "id", "name", "percentage", "level", "level_text", "icon", "bar_style", "is_new", "enabled"))
                .addSchemas("ProjectsContent", collectionSchema("items", "项目卡片及详情",
                        "id", "card_title", "card_summary", "detail_title", "detail_summary", "tag", "accent", "year",
                        "image", "image_alt", "paragraphs", "tech", "images", "lab_post_id", "enabled"))
                .addSchemas("FootprintsContent", collectionSchema("details", "按前端稳定城市 ID 关联的详情",
                        "id", "title", "summary", "paragraphs", "images", "cta_text", "cta_url"))
                .addSchemas("HobbiesContent", collectionSchema("cards", "右侧爱好卡片",
                        "id", "title", "description", "image", "image_alt", "enabled"))
                .addSchemas("VibeContent", collectionSchema("tools", "右侧 AI 工具",
                        "id", "name", "percentage", "description", "enabled"))
                .addSchemas("MyLabContent", myLabSchema())
                .addSchemas("SupportDraftContent", supportDraftSchema())
                .addSchemas("SupportPublicContent", supportPublicSchema());
        return new OpenAPI().components(components);
    }

    private Schema<?> collectionSchema(String field, String description, String... itemFields) {
        return new ObjectSchema().description(description)
                .addProperty(field, new ArraySchema().items(itemSchema(itemFields)));
    }

    private ObjectSchema itemSchema(String... fields) {
        ObjectSchema item = new ObjectSchema();
        for (String field : fields) item.addProperty(field, new Schema<>().description(field));
        return item;
    }

    private Schema<?> myLabSchema() {
        return new ObjectSchema().description("MyLab 标签、研究记录卡片与章节详情")
                .addProperty("tags", new ArraySchema().items(itemSchema("id", "name", "enabled")))
                .addProperty("posts", new ArraySchema().items(itemSchema(
                        "id", "date", "title", "tags", "summary", "image", "image_alt", "sections", "enabled")));
    }

    private Schema<?> supportDraftSchema() {
        return new ObjectSchema().description("支持页后台草稿，仅保存三个非负统计数值")
                .addProperty("visit_base", new IntegerSchema().format("int64").minimum(java.math.BigDecimal.ZERO))
                .addProperty("like_count", new IntegerSchema().format("int64").minimum(java.math.BigDecimal.ZERO))
                .addProperty("page_view_base", new IntegerSchema().format("int64").minimum(java.math.BigDecimal.ZERO));
    }

    private Schema<?> supportPublicSchema() {
        return new ObjectSchema().description("支持页公开计算结果")
                .addProperty("visit_count", new IntegerSchema().format("int64"))
                .addProperty("like_count", new IntegerSchema().format("int64"))
                .addProperty("page_view_count", new IntegerSchema().format("int64"));
    }

    @Bean
    public OperationCustomizer standardErrorResponses() {
        Map<String, ErrorExample> definitions = new LinkedHashMap<>();
        definitions.put("400", new ErrorExample(10009, "请求体格式错误", "请求格式或参数类型不正确"));
        definitions.put("401", new ErrorExample(10001, "身份认证失败", null));
        definitions.put("403", new ErrorExample(10004, "无权执行该操作", null));
        definitions.put("404", new ErrorExample(10005, "请求的资源不存在", null));
        definitions.put("409", new ErrorExample(10006, "资源状态冲突", null));
        definitions.put("405", new ErrorExample(10010, "请求方法不支持", null));
        definitions.put("413", new ErrorExample(13003, "上传文件超过大小限制", null));
        definitions.put("415", new ErrorExample(10011, "请求媒体类型不支持", null));
        definitions.put("422", new ErrorExample(10007, "请求参数校验失败", "字段不符合约束"));
        definitions.put("429", new ErrorExample(10008, "请求过于频繁", null));
        definitions.put("500", new ErrorExample(20001, "服务器内部错误", null));
        definitions.put("503", new ErrorExample(13005, "文件存储服务不可用", null));

        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            definitions.forEach((status, definition) -> responses.putIfAbsent(status,
                    errorResponse(definition)));
            return operation;
        };
    }

    private ApiResponse errorResponse(ErrorExample definition) {
        io.swagger.v3.oas.models.media.MediaType mediaType = new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))
                .example(example(definition));
        return new ApiResponse()
                .description(definition.message())
                .content(new io.swagger.v3.oas.models.media.Content()
                        .addMediaType("application/json", mediaType));
    }

    private Map<String, Object> example(ErrorExample definition) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", definition.code());
        value.put("message", definition.message());
        if (definition.detail() != null) value.put("error", definition.detail());
        value.put("request_id", "01J5MYBLOG7P8ABCDEF12345678");
        value.put("timestamp", 1785832213L);
        return value;
    }

    private record ErrorExample(int code, String message, String detail) {
    }
}
