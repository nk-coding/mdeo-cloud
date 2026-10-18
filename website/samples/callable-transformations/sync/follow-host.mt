using "../microservices.mm"

// A local repair of one element: puts a component on a container, unless it is there already.
transformation followHost(in component: Component, in host: Container)

if match {
    component { }
    host { }
    old: Container { }
    delete component.host -- old
    create component.host -- host
    forbid {
        component.host -- host
    }
} then {
}
