# Maven + Javassist 编译期动态注入代码

> 感谢 https://github.com/stephanenicolas/javassist-build-plugin-api

## 这是一个maven插件，用于在maven编译时动态注入代码。

> 常见使用场景：License授权检测埋点代码，可以在一些核心的方法之前插入一段License有效性检测，
> 由于是动态随机可控的埋点插入，可以增加License破解复杂度和时间（例如：随机选择100个方法进行埋点代码插入）。

## 使用方式：

1. 项目的 `pom.xml` 文件中配置插件：
 
```xml
<!-- 增加依赖 -->
<dependency>
    <groupId>org.javassist</groupId>
    <artifactId>javassist</artifactId>
    <version>3.30.2-GA</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>com.gdk</groupId>
    <artifactId>injector-maven-plugin</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>

<!-- 增加插件 -->
<plugin>
    <groupId>com.gdk</groupId>
    <artifactId>injector-maven-plugin</artifactId>
    <version>0.2</version>
    <configuration>
        <includeTestClasses>false</includeTestClasses>
        <transformerClasses>
            <!-- 这里配置自己的字节码转换器 -->
            <transformerClass>
                <className>demo.license.InsertCodeTransformer</className>
                <properties>
                    <property>
                        <name>code</name>
                        <value>{ System.out.println("*** 我是动态插入的 ***"); }</value>
                    </property>
                </properties>
            </transformerClass>
        </transformerClasses>
    </configuration>
    <executions>
        <execution>
            <phase>process-classes</phase>
            <goals>
                <goal>injector</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- 可选：可以配置 maven-jar-plugin 插件，将转换器代码从jar包排除 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>demo/license/**</exclude>
        </excludes>
    </configuration>
</plugin>

```
2. 实现自己的字节码转换器，例如：

```java
package demo.license;

import java.util.Properties;

import com.gdk.maven.plugin.injector.ClassTransformer;
import demo.DemoController;
import javassist.*;

/**
 * 示例：在 `DemoController` 类的每个方法前面插入配置的代码。
 */
public class InsertCodeTransformer extends ClassTransformer {
    private String myCode;

    @Override
    public boolean shouldTransform(final CtClass candidateClass) {
        // 只处理 DemoController 这个类
        try {
            CtClass myInterface = ClassPool.getDefault().get(DemoController.class.getName());
            return !candidateClass.equals(myInterface) && candidateClass.subtypeOf(myInterface);
        } catch (Exception e) {
            //ignore
        }
        return false;
    }

    @Override
    public void applyTransformations(CtClass classToTransform) {
        if (this.myCode == null || this.myCode.isEmpty()) {
            return;
        }

        // 获取这个类的所有方法
        CtMethod[] declaredMethods = classToTransform.getDeclaredMethods();
        try {
            for (CtMethod m : declaredMethods) {
                // 在方法前面插入配置的一段代码
                m.insertBefore(this.myCode);
            }
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configure(final Properties properties) {
        if (null != properties) {
            // 从插件的配置项中获取配置的代码
            this.myCode = properties.getProperty("code");
        }
    }
}
```

3. 上述步骤完成后，运行 `maven compile` 后，你会发现编译的class代码中含有了动态插入的代码。