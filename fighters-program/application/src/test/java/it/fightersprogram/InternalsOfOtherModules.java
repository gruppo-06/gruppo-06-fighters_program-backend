package it.fightersprogram;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class InternalsOfOtherModules extends ArchCondition<JavaClass> {

    private static final String BASE = "it.fightersprogram.";

    private final String[] modules;

    InternalsOfOtherModules(String... modules) {
        super("depend on web or domain classes of another module");
        this.modules = modules;
    }

    @Override
    public void check(JavaClass origin, ConditionEvents events) {
        origin.getDirectDependenciesFromSelf().forEach(dependency -> {
            String target = dependency.getTargetClass().getPackageName();
            for (String module : modules) {
                boolean targetIsInternal = target.startsWith(BASE + module + ".web")
                        || target.startsWith(BASE + module + ".domain");
                boolean originInsideModule = origin.getPackageName().startsWith(BASE + module);
                if (targetIsInternal && !originInsideModule) {
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        });
    }
}
