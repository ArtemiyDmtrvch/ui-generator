package ru.impression.ui_generator_processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class FragmentComponentClassBuilder(
    scheme: TypeElement,
    resultClassName: String,
    resultClassPackage: String,
    superclass: TypeName,
    viewModelClass: TypeMirror
) : ComponentClassBuilder(
    scheme,
    resultClassName,
    resultClassPackage,
    superclass,
    viewModelClass
) {

    override fun buildViewModelProperty() =
        with(PropertySpec.builder("viewModel", viewModelClass.asTypeName())) {
            addModifiers(KModifier.OVERRIDE)
            delegate(if (propProperties.isEmpty()) CodeBlock.of("lazy { createViewModel($viewModelClass::class) } ") else
                with(CodeBlock.builder()) {
                    add(
                        """
                        lazy { 
                          val viewModel = createViewModel($viewModelClass::class)
                        
                        """.trimIndent()
                    )
                    propProperties.forEach {
                        add(
                            """
                                val ${it.name} = ${it.name}
                                if (${it.name} != null && ${it.name} !== viewModel.${it.name})
                                  viewModel::${it.name}.%M(${it.name})
                                  viewModel.onStateChanged(renderImmediately = true)

                                  """.trimIndent(),
                            MemberName("ru.impression.ui_generator_base", "nullSafetySet")
                        )
                    }
                    add(
                        """
                            viewModel
                            }
                        """.trimIndent()
                    )
                    build()
                })
            build()
        }

    override fun buildContainerProperty() =
        with(PropertySpec.builder("container", ClassName("android.view", "View").copy(true))) {
            mutable(true)
            addModifiers(KModifier.OVERRIDE)
            initializer("null")
            build()
        }

    override fun buildBoundLifecycleOwnerProperty() = with(
        PropertySpec.builder(
            "boundLifecycleOwner",
            ClassName("androidx.lifecycle", "LifecycleOwner")
        )
    ) {
        addModifiers(KModifier.OVERRIDE)
        getter(FunSpec.getterBuilder().addCode("return viewLifecycleOwner").build())
        build()
    }

    override fun TypeSpec.Builder.addRestMembers() {
        propProperties.forEach { addProperty(buildPropWrapperProperty(it)) }
        addFunction(buildOnCreateFunction())
        addFunction(buildOnCreateViewFunction())
        addFunction(buildOnActivityCreatedFunction())
        addFunction(buildOnSaveInstanceStateFunction())
        addFunction(buildOnDestroyViewFunction())
    }

    private fun buildPropWrapperProperty(propProperty: PropProperty) = with(
        PropertySpec.builder(
            propProperty.name,
            propProperty.kotlinType
        )
    ) {
        mutable(true)
        initializer("null")
        getter(
            FunSpec.getterBuilder()
                .addCode(
                    """
                        return field ?: arguments?.get("${propProperty.name}") as? ${propProperty.kotlinType}
                    
                    """.trimIndent()
                )
                .build()
        )
        setter(
            FunSpec.setterBuilder().addParameter("value", propProperty.kotlinType).addCode(
                """
                    field = value
                    try {
                      %M("${propProperty.name}", value)
                    } catch (e: %T) {
                    }
                """.trimIndent(),
                MemberName("ru.impression.ui_generator_base", "putArgument"),
                IllegalArgumentException::class.java
            ).build()
        )
        build()
    }

    private fun buildOnCreateFunction() = with(FunSpec.builder("onCreate")) {
        addModifiers(KModifier.OVERRIDE)
        addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(true))
        addCode(
            """
                super.onCreate(savedInstanceState)
                viewModel.onRestoreInstanceState(savedInstanceState?.getParcelable("viewModelState"))""".trimIndent()
        )
        build()
    }

    private fun buildOnCreateViewFunction() = with(FunSpec.builder("onCreateView")) {
        addModifiers(KModifier.OVERRIDE)
        addParameter("inflater", ClassName("android.view", "LayoutInflater"))
        addParameter("container", ClassName("android.view", "ViewGroup").copy(true))
        addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(true))
        returns(ClassName("android.view", "View").copy(true))
        addCode(
            """
                this.container = container
                return render(attachToContainer = false, executeBindingsImmediately = false)?.root
                """.trimIndent()
        )
        build()
    }

    private fun buildOnActivityCreatedFunction() = with(FunSpec.builder("onActivityCreated")) {
        addModifiers(KModifier.OVERRIDE)
        addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(true))
        addCode(
            """
                super.onActivityCreated(savedInstanceState)
                viewModel.setComponent(this)
                """.trimIndent()
        )
        build()
    }

    private fun buildOnSaveInstanceStateFunction() = with(FunSpec.builder("onSaveInstanceState")) {
        addModifiers(KModifier.OVERRIDE)
        addParameter("outState", ClassName("android.os", "Bundle"))
        addCode(
            """
                super.onSaveInstanceState(outState)
                outState.putParcelable("viewModelState", viewModel.onSaveInstanceState())
                """.trimIndent()
        )
        build()
    }

    private fun buildOnDestroyViewFunction() = with(FunSpec.builder("onDestroyView")) {
        addModifiers(KModifier.OVERRIDE)
        addCode(
            """
                super.onDestroyView()
                dataBindingManager.releaseBinding()
                """.trimIndent()
        )
        build()
    }
}