import { openBlock as w, createElementBlock as ee, createElementVNode as I, ref as m, computed as de, watch as ve, onMounted as me, resolveComponent as u, toDisplayString as p, unref as d, createVNode as a, withCtx as t, createTextVNode as v, createBlock as $, createCommentVNode as A, withDirectives as pe, withModifiers as B, vShow as fe } from "vue";
import { useApi as _e, useAppStore as ge, useErrorStore as ye, useI18nStore as ke } from "@ligoj/host";
const we = (l, r) => {
  const s = l.__vccOpts || l;
  for (const [i, f] of r)
    s[i] = f;
  return s;
}, be = { class: "plugin-id" }, he = {
  __name: "IdPlugin",
  setup(l) {
    return (r, s) => (w(), ee("div", be, [...s[0] || (s[0] = [
      I("h2", null, "Identity", -1),
      I("p", null, "Plugin shell — views (users, groups, companies, delegates, container-scopes) move here in slice 3b.", -1)
    ])]));
  }
}, Se = /* @__PURE__ */ we(he, [["__scopeId", "data-v-6edf097d"]]), xe = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, Ce = { class: "text-h4" }, Ve = {
  __name: "ContainerScopeView",
  setup(l) {
    const r = _e(), s = ge(), i = ye(), e = ke().t, _ = m("group"), j = [
      { id: 1, name: "Department", locked: !1 },
      { id: 2, name: "Team", locked: !1 },
      { id: 3, name: "Project", locked: !0 }
    ], F = [
      { id: 1, name: "Organization", locked: !1 },
      { id: 2, name: "Business Unit", locked: !0 }
    ], g = m([]), O = m(0), C = m(!1), T = m(null), b = m(!1), te = de(() => [
      { title: e("common.name"), key: "name", sortable: !0 },
      { title: e("common.status"), key: "locked", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]), P = m(null), y = m(!1), V = m(null), U = m({ name: "" }), D = m(!1), h = m(!1), M = m(null), q = m(!1), ae = { required: (c) => !!c || e("common.required") };
    async function E() {
      C.value = !0, T.value = null;
      try {
        const c = await r.get(`rest/service/id/container-scope/${_.value}`);
        c && !c.code ? (g.value = Array.isArray(c) ? c : c.data || [], O.value = g.value.length, b.value = !1) : (b.value = !0, i.clear(), g.value = _.value === "group" ? j : F, O.value = g.value.length);
      } catch {
        b.value = !0, i.clear(), g.value = _.value === "group" ? j : F, O.value = g.value.length;
      }
      C.value = !1;
    }
    ve(_, () => {
      E();
    });
    function ne() {
      V.value = null, U.value = { name: "" }, y.value = !0;
    }
    function G(c) {
      V.value = c, U.value = { name: c.name }, y.value = !0;
    }
    function le(c) {
      M.value = c, h.value = !0;
    }
    async function Y() {
      var S;
      const { valid: c } = await P.value.validate();
      if (!c) return;
      if (b.value) {
        i.push({ message: e("containerScope.demoSave"), status: 0 }), y.value = !1;
        return;
      }
      D.value = !0;
      const n = { name: U.value.name };
      (S = V.value) != null && S.id ? await r.put(`rest/service/id/container-scope/${_.value}`, { id: V.value.id, ...n }) : await r.post(`rest/service/id/container-scope/${_.value}`, n), D.value = !1, y.value = !1, E();
    }
    async function oe() {
      if (b.value) {
        i.push({ message: e("containerScope.demoDelete"), status: 0 }), h.value = !1;
        return;
      }
      q.value = !0, await r.del(`rest/service/id/container-scope/${_.value}/${M.value.id}`), q.value = !1, h.value = !1, E();
    }
    return me(() => {
      s.setTitle(e("containerScope.title")), s.setBreadcrumbs([
        { title: e("nav.home"), to: "/" },
        { title: e("nav.identity") },
        { title: e("containerScope.title") }
      ]), E();
    }), (c, n) => {
      const S = u("v-spacer"), k = u("v-btn"), H = u("v-tab"), re = u("v-tabs"), J = u("v-alert"), se = u("v-skeleton-loader"), z = u("v-icon"), ie = u("v-data-table"), K = u("v-card-title"), ce = u("v-text-field"), ue = u("v-form"), L = u("v-card-text"), Q = u("v-card-actions"), W = u("v-card"), X = u("v-dialog");
      return w(), ee("div", null, [
        I("div", xe, [
          I("h1", Ce, p(d(e)("containerScope.title")), 1),
          a(S),
          a(k, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: ne
          }, {
            default: t(() => [
              v(p(d(e)("containerScope.new")), 1)
            ]),
            _: 1
          })
        ]),
        a(re, {
          modelValue: _.value,
          "onUpdate:modelValue": n[0] || (n[0] = (o) => _.value = o),
          class: "mb-4"
        }, {
          default: t(() => [
            a(H, { value: "group" }, {
              default: t(() => [
                v(p(d(e)("nav.groups")), 1)
              ]),
              _: 1
            }),
            a(H, { value: "company" }, {
              default: t(() => [
                v(p(d(e)("nav.companies")), 1)
              ]),
              _: 1
            })
          ]),
          _: 1
        }, 8, ["modelValue"]),
        T.value ? (w(), $(J, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            v(p(d(e)("containerScope.noProvider")), 1)
          ]),
          _: 1
        })) : A("", !0),
        b.value ? (w(), $(J, {
          key: 1,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            v(p(d(e)("containerScope.demoMode")), 1)
          ]),
          _: 1
        })) : A("", !0),
        C.value && g.value.length === 0 ? (w(), $(se, {
          key: 2,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : A("", !0),
        T.value ? A("", !0) : pe((w(), $(ie, {
          key: 3,
          headers: te.value,
          items: g.value,
          loading: C.value,
          "item-value": "id",
          hover: "",
          "onClick:row": n[1] || (n[1] = (o, { item: N }) => G(N))
        }, {
          "item.locked": t(({ item: o }) => [
            o.locked ? (w(), $(z, {
              key: 0,
              color: "warning",
              size: "small"
            }, {
              default: t(() => [...n[7] || (n[7] = [
                v("mdi-lock", -1)
              ])]),
              _: 1
            })) : A("", !0)
          ]),
          "item.actions": t(({ item: o }) => [
            a(k, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: B((N) => G(o), ["stop"])
            }, {
              default: t(() => [
                a(z, { size: "small" }, {
                  default: t(() => [...n[8] || (n[8] = [
                    v("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            a(k, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: B((N) => le(o), ["stop"]),
              disabled: o.locked
            }, {
              default: t(() => [
                a(z, { size: "small" }, {
                  default: t(() => [...n[9] || (n[9] = [
                    v("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick", "disabled"])
          ]),
          _: 1
        }, 8, ["headers", "items", "loading"])), [
          [fe, g.value.length > 0 || !C.value]
        ]),
        a(X, {
          modelValue: y.value,
          "onUpdate:modelValue": n[4] || (n[4] = (o) => y.value = o),
          "max-width": "500"
        }, {
          default: t(() => [
            a(W, null, {
              default: t(() => [
                a(K, null, {
                  default: t(() => {
                    var o;
                    return [
                      v(p((o = V.value) != null && o.id ? d(e)("containerScope.edit") : d(e)("containerScope.new")), 1)
                    ];
                  }),
                  _: 1
                }),
                a(L, null, {
                  default: t(() => [
                    a(ue, {
                      ref_key: "formRef",
                      ref: P,
                      onSubmit: B(Y, ["prevent"])
                    }, {
                      default: t(() => [
                        a(ce, {
                          modelValue: U.value.name,
                          "onUpdate:modelValue": n[2] || (n[2] = (o) => U.value.name = o),
                          label: d(e)("common.name"),
                          rules: [ae.required],
                          variant: "outlined",
                          class: "mb-2"
                        }, null, 8, ["modelValue", "label", "rules"])
                      ]),
                      _: 1
                    }, 512)
                  ]),
                  _: 1
                }),
                a(Q, null, {
                  default: t(() => [
                    a(S),
                    a(k, {
                      variant: "text",
                      onClick: n[3] || (n[3] = (o) => y.value = !1)
                    }, {
                      default: t(() => [
                        v(p(d(e)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    a(k, {
                      color: "primary",
                      variant: "elevated",
                      loading: D.value,
                      onClick: Y
                    }, {
                      default: t(() => [
                        v(p(d(e)("common.save")), 1)
                      ]),
                      _: 1
                    }, 8, ["loading"])
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })
          ]),
          _: 1
        }, 8, ["modelValue"]),
        a(X, {
          modelValue: h.value,
          "onUpdate:modelValue": n[6] || (n[6] = (o) => h.value = o),
          "max-width": "400"
        }, {
          default: t(() => [
            a(W, null, {
              default: t(() => [
                a(K, null, {
                  default: t(() => [
                    v(p(d(e)("containerScope.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                a(L, null, {
                  default: t(() => {
                    var o;
                    return [
                      v(p(d(e)("containerScope.deleteConfirm", { name: (o = M.value) == null ? void 0 : o.name })), 1)
                    ];
                  }),
                  _: 1
                }),
                a(Q, null, {
                  default: t(() => [
                    a(S),
                    a(k, {
                      variant: "text",
                      onClick: n[5] || (n[5] = (o) => h.value = !1)
                    }, {
                      default: t(() => [
                        v(p(d(e)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    a(k, {
                      color: "error",
                      variant: "elevated",
                      loading: q.value,
                      onClick: oe
                    }, {
                      default: t(() => [
                        v(p(d(e)("common.delete")), 1)
                      ]),
                      _: 1
                    }, 8, ["loading"])
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })
          ]),
          _: 1
        }, 8, ["modelValue"])
      ]);
    };
  }
}, R = "/rest/", x = {
  requireAgreement(l) {
    return !l || !l["security-agreement"];
  },
  async acceptAgreement(l) {
    if (!(await fetch(R + "system/setting/security-agreement/1", {
      method: "POST",
      credentials: "include"
    })).ok) throw new Error("Failed to accept agreement");
    return l && (l["security-agreement"] = !0), !0;
  },
  scheduleUpload(l, r, s, i) {
    const f = setInterval(
      () => x._syncUpload(l, r, s, i, f),
      1e3
    );
    return f;
  },
  async _syncUpload(l, r, s, i, f) {
    try {
      const e = await fetch(R + l + "/" + r + "/status", { credentials: "include" });
      if (!e.ok) return;
      const _ = await e.json();
      i == null || i(_), _.end && (clearInterval(f), await x._finishUpload(l, r, s, i));
    } catch (e) {
      console.error("[plugin:id] upload sync error", e);
    }
  },
  async _finishUpload(l, r, s, i) {
    try {
      const f = await fetch(R + l + "/" + r, { credentials: "include" });
      if (!f.ok) return;
      const e = await f.json();
      i == null || i({ ...e.status, finished: !0, errors: e.entries }), s == null || s(e);
    } catch (f) {
      console.error("[plugin:id] upload result error", f);
    }
  }
}, Ue = {
  requireAgreement: x.requireAgreement,
  acceptAgreement: x.acceptAgreement,
  scheduleUpload: x.scheduleUpload
}, Z = [
  { path: "/id/container-scope", name: "id-container-scope", component: Ve }
], Ee = {
  id: "id",
  label: "Identity",
  component: Se,
  routes: Z,
  install({ router: l }) {
    for (const r of Z)
      l.addRoute(r);
  },
  feature(l, ...r) {
    const s = Ue[l];
    if (!s) throw new Error(`Plugin "id" has no feature "${l}"`);
    return s(...r);
  },
  service: x,
  meta: { icon: "mdi-account-group", color: "blue-darken-3" }
};
export {
  Ee as default,
  x as service
};
