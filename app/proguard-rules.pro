# 保持数据模型不被混淆 (Gson 序列化需要)
-keep class com.project.medi_agent.ui.ChatMessage { *; }
-keep class com.project.medi_agent.ui.ChatSession { *; }
-keep class com.project.medi_agent.data.network.ChatMessageRequest { *; }
-keep class com.project.medi_agent.data.network.ChatCompletionRequest { *; }
-keep class com.project.medi_agent.data.network.ChatCompletionResponse { *; }
-keep class com.project.medi_agent.data.network.ChatChoice { *; }
-keep class com.project.medi_agent.data.network.ChatMessageResponse { *; }
-keep class com.project.medi_agent.data.network.ChatDelta { *; }
-keep class com.project.medi_agent.data.network.ChatThinkingConfig { *; }

# 保持 Retrofit 相关的注解和接口
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface com.project.medi_agent.data.network.** { *; }

# 优化后的 CommonMark 规则
# 仅保持解析器和节点基类，以及我们通过 'is' 判断的所有子类
-keep class org.commonmark.parser.Parser { *; }
-keep class org.commonmark.parser.Parser$Builder { *; }
-keep class org.commonmark.node.Node { *; }
-keep class * extends org.commonmark.node.Node
