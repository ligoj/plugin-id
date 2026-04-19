import { openBlock as k, createElementBlock as ne, createElementVNode as te, ref as g, computed as ie, onMounted as se, resolveComponent as i, toDisplayString as n, unref as l, createVNode as e, withCtx as t, createTextVNode as a, createBlock as h, createCommentVNode as U, withDirectives as ye, withModifiers as oe, Fragment as Ne, renderList as Ee, vShow as ke, isRef as be, watch as Me } from "vue";
import { useRouter as ve, useRoute as we } from "vue-router";
import { useAppStore as re, useApi as ue, useErrorStore as _e, useI18nStore as de, useDataTable as Ve, ImportExportBar as Pe, useFormGuard as xe } from "@ligoj/host";
const ce = (z, b) => {
  const E = z.__vccOpts || z;
  for (const [w, A] of b)
    E[w] = A;
  return E;
}, Ae = { class: "plugin-id" }, Oe = {
  __name: "IdPlugin",
  setup(z) {
    return (b, E) => (k(), ne("div", Ae, [...E[0] || (E[0] = [
      te("h2", null, "Identity", -1),
      te("p", null, "Plugin shell — views (users, groups, companies, delegates, container-scopes) move here in slice 3b.", -1)
    ])]));
  }
}, Re = /* @__PURE__ */ ce(Oe, [["__scopeId", "data-v-6edf097d"]]), Ge = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, Le = { class: "text-h4" }, ze = {
  key: 0,
  class: "text-caption text-medium-emphasis"
}, Be = {
  __name: "UserListView",
  setup(z) {
    const b = ve(), E = re(), w = ue(), A = _e(), o = de().t, c = Ve("service/id/user", { defaultSort: "id", demoData: [
      { id: "admin", firstName: "Admin", lastName: "User", company: "Ligoj", mails: ["admin@ligoj.org"], groups: [{ name: "Engineering" }, { name: "Management" }], locked: !1 },
      { id: "jdupont", firstName: "Jean", lastName: "Dupont", company: "Ligoj", mails: ["jean.dupont@ligoj.org"], groups: [{ name: "Engineering" }, { name: "DevOps" }], locked: !1 },
      { id: "mmartin", firstName: "Marie", lastName: "Martin", company: "AcmeCorp", mails: ["marie.martin@acme.com"], groups: [{ name: "Marketing" }], locked: !1 },
      { id: "pdurand", firstName: "Pierre", lastName: "Durand", company: "AcmeCorp", mails: ["pierre.durand@acme.com"], groups: [{ name: "Engineering" }], locked: !1 },
      { id: "sleblanc", firstName: "Sophie", lastName: "Leblanc", company: "TechSolutions", mails: ["sophie.leblanc@techsol.com"], groups: [{ name: "DevOps" }], locked: !1 },
      { id: "tmoreau", firstName: "Thomas", lastName: "Moreau", company: "TechSolutions", mails: ["thomas.moreau@techsol.com"], groups: [{ name: "Sales" }], locked: !1 },
      { id: "crichard", firstName: "Claire", lastName: "Richard", company: "Ligoj", mails: ["claire.richard@ligoj.org"], groups: [{ name: "Management" }], locked: !1 },
      { id: "agarcia", firstName: "Antoine", lastName: "Garcia", company: "Ligoj", mails: ["antoine.garcia@ligoj.org"], groups: [{ name: "Engineering" }], locked: !1 }
    ] }), D = g(25);
    let L = null;
    const x = g([]), V = g(!1), S = g(null), P = g(!1), m = g(!1), M = ie(() => [
      { title: o("user.login"), key: "id", sortable: !0 },
      { title: o("user.firstName"), key: "firstName", sortable: !0 },
      { title: o("user.lastName"), key: "lastName", sortable: !0 },
      { title: o("user.company"), key: "company", sortable: !0 },
      { title: o("user.email"), key: "mails", sortable: !1 },
      { title: o("user.groups"), key: "groups", sortable: !1 },
      { title: o("common.status"), key: "locked", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]);
    function F(q) {
      c.load(q);
    }
    function I() {
      clearTimeout(L), L = setTimeout(() => c.load({ page: 1, itemsPerPage: D.value }), 300);
    }
    function ee(q) {
      S.value = q, V.value = !0;
    }
    async function B() {
      if (c.demoMode.value) {
        A.push({ message: o("user.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      P.value = !0, await w.del(`rest/service/id/user/${S.value.id}`), P.value = !1, V.value = !1, S.value = null, c.load({ page: 1, itemsPerPage: D.value });
    }
    function le() {
      m.value = !0;
    }
    async function $() {
      if (c.demoMode.value) {
        A.push({ message: o("user.demoDelete"), status: 0 }), m.value = !1;
        return;
      }
      P.value = !0;
      for (const q of x.value)
        await w.del(`rest/service/id/user/${q}`);
      P.value = !1, m.value = !1, x.value = [], c.load({ page: 1, itemsPerPage: D.value });
    }
    return se(() => {
      E.setTitle(o("user.title")), E.setBreadcrumbs([
        { title: o("nav.home"), to: "/" },
        { title: o("nav.identity") },
        { title: o("user.title") }
      ]);
    }), (q, d) => {
      const f = i("v-spacer"), _ = i("v-text-field"), y = i("v-btn"), p = i("v-alert-title"), Q = i("v-alert"), G = i("v-toolbar-title"), O = i("v-toolbar"), J = i("v-slide-y-transition"), X = i("v-skeleton-loader"), R = i("v-chip"), Y = i("v-icon"), T = i("v-data-table-server"), u = i("v-card-title"), W = i("v-card-text"), K = i("v-card-actions"), C = i("v-card"), N = i("v-dialog");
      return k(), ne("div", null, [
        te("div", Ge, [
          te("h1", Le, n(l(o)("user.title")), 1),
          e(f),
          e(_, {
            modelValue: l(c).search.value,
            "onUpdate:modelValue": [
              d[0] || (d[0] = (r) => l(c).search.value = r),
              I
            ],
            "prepend-inner-icon": "mdi-magnify",
            label: l(o)("common.search"),
            variant: "outlined",
            density: "compact",
            "hide-details": "",
            class: "search-field"
          }, null, 8, ["modelValue", "label"]),
          e(y, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: d[1] || (d[1] = (r) => l(b).push("/id/user/new"))
          }, {
            default: t(() => [
              a(n(l(o)("user.new")), 1)
            ]),
            _: 1
          }),
          e(l(Pe), {
            "export-endpoint": "service/id/user",
            "import-endpoint": "service/id/user/import/csv/full",
            "export-filename": "users.csv",
            onImported: d[2] || (d[2] = (r) => l(c).load({ page: 1, itemsPerPage: D.value.value }))
          })
        ]),
        l(c).error.value ? (k(), h(Q, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            e(p, null, {
              default: t(() => [
                a(n(l(o)("user.noProvider")), 1)
              ]),
              _: 1
            }),
            a(" " + n(l(c).error.value === "internal" ? l(o)("user.noProviderMsg") : l(c).error.value), 1)
          ]),
          _: 1
        })) : U("", !0),
        l(c).demoMode.value ? (k(), h(Q, {
          key: 1,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(o)("user.demoMode")), 1)
          ]),
          _: 1
        })) : U("", !0),
        e(J, null, {
          default: t(() => [
            x.value.length ? (k(), h(O, {
              key: 0,
              density: "compact",
              color: "primary",
              rounded: "",
              class: "mb-4"
            }, {
              default: t(() => [
                e(G, null, {
                  default: t(() => [
                    a(n(x.value.length) + " " + n(l(o)("common.selected")), 1)
                  ]),
                  _: 1
                }),
                e(f),
                e(y, {
                  variant: "elevated",
                  color: "error",
                  "prepend-icon": "mdi-delete",
                  onClick: le
                }, {
                  default: t(() => [
                    a(n(l(o)("common.delete")), 1)
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })) : U("", !0)
          ]),
          _: 1
        }),
        l(c).loading.value && l(c).items.value.length === 0 ? (k(), h(X, {
          key: 2,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : U("", !0),
        l(c).error.value ? U("", !0) : ye((k(), h(T, {
          key: 3,
          modelValue: x.value,
          "onUpdate:modelValue": d[3] || (d[3] = (r) => x.value = r),
          "items-per-page": D.value,
          "onUpdate:itemsPerPage": d[4] || (d[4] = (r) => D.value = r),
          headers: M.value,
          items: l(c).items.value,
          "items-length": l(c).totalItems.value,
          loading: l(c).loading.value,
          "item-value": "id",
          "show-select": "",
          hover: "",
          "onUpdate:options": F,
          "onClick:row": d[5] || (d[5] = (r, { item: j }) => l(b).push("/id/user/" + j.id))
        }, {
          "item.mails": t(({ item: r }) => {
            var j;
            return [
              a(n(((j = r.mails) == null ? void 0 : j[0]) || ""), 1)
            ];
          }),
          "item.groups": t(({ item: r }) => [
            (k(!0), ne(Ne, null, Ee((r.groups || []).slice(0, 3), (j) => (k(), h(R, {
              key: j.name || j,
              size: "small",
              class: "mr-1"
            }, {
              default: t(() => [
                a(n(j.name || j), 1)
              ]),
              _: 2
            }, 1024))), 128)),
            (r.groups || []).length > 3 ? (k(), ne("span", ze, " +" + n(r.groups.length - 3), 1)) : U("", !0)
          ]),
          "item.locked": t(({ item: r }) => [
            r.locked ? (k(), h(Y, {
              key: 0,
              color: "error",
              size: "small"
            }, {
              default: t(() => [...d[10] || (d[10] = [
                a("mdi-lock", -1)
              ])]),
              _: 1
            })) : (k(), h(Y, {
              key: 1,
              color: "success",
              size: "small"
            }, {
              default: t(() => [...d[11] || (d[11] = [
                a("mdi-lock-open-variant", -1)
              ])]),
              _: 1
            }))
          ]),
          "item.actions": t(({ item: r }) => [
            e(y, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: oe((j) => l(b).push("/id/user/" + r.id), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...d[12] || (d[12] = [
                    a("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            e(y, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: oe((j) => ee(r), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...d[13] || (d[13] = [
                    a("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"])
          ]),
          _: 1
        }, 8, ["modelValue", "items-per-page", "headers", "items", "items-length", "loading"])), [
          [ke, l(c).items.value.length > 0 || !l(c).loading.value]
        ]),
        e(N, {
          modelValue: V.value,
          "onUpdate:modelValue": d[7] || (d[7] = (r) => V.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("user.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => {
                    var r;
                    return [
                      a(n(l(o)("user.deleteConfirm", { id: (r = S.value) == null ? void 0 : r.id })), 1)
                    ];
                  }),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(f),
                    e(y, {
                      variant: "text",
                      onClick: d[6] || (d[6] = (r) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(y, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: B
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
        e(N, {
          modelValue: m.value,
          "onUpdate:modelValue": d[9] || (d[9] = (r) => m.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteConfirm", { count: x.value.length })), 1)
                  ]),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(f),
                    e(y, {
                      variant: "text",
                      onClick: d[8] || (d[8] = (r) => m.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(y, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: $
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
}, je = /* @__PURE__ */ ce(Be, [["__scopeId", "data-v-215250fd"]]), Ie = { class: "d-flex align-center mb-4" }, qe = { class: "text-h4" }, We = { class: "d-flex flex-wrap ga-2" }, Fe = {
  __name: "UserEditView",
  setup(z) {
    const b = we(), E = ve(), w = ue(), A = re(), v = _e(), s = de().t, c = g(null), D = g(!1), L = g(!1), x = g(!1), V = g(!1), S = g(!1), P = g([]), m = g(!1), M = g(!1), F = g(!1), I = g(""), ee = g(!1), B = ie(() => !!b.params.id), le = ie(() => P.value.map((T) => T.name || T).join(", ") || "-"), $ = g({
      id: "",
      firstName: "",
      lastName: "",
      company: "",
      mail: ""
    }), { isDirty: q, showGuardDialog: d, confirmLeave: f, cancelLeave: _, markClean: y, init: p } = xe($), Q = {
      required: (T) => !!T || s("common.required")
    }, G = [
      { id: "admin", firstName: "Admin", lastName: "User", company: "Ligoj", mails: ["admin@ligoj.org"], groups: [{ name: "Engineering" }, { name: "Management" }] },
      { id: "jdupont", firstName: "Jean", lastName: "Dupont", company: "Ligoj", mails: ["jean.dupont@ligoj.org"], groups: [{ name: "Engineering" }, { name: "DevOps" }] },
      { id: "mmartin", firstName: "Marie", lastName: "Martin", company: "AcmeCorp", mails: ["marie.martin@acme.com"], groups: [{ name: "Marketing" }] },
      { id: "pdurand", firstName: "Pierre", lastName: "Durand", company: "AcmeCorp", mails: ["pierre.durand@acme.com"], groups: [{ name: "Engineering" }] },
      { id: "sleblanc", firstName: "Sophie", lastName: "Leblanc", company: "TechSolutions", mails: ["sophie.leblanc@techsol.com"], groups: [{ name: "DevOps" }] },
      { id: "tmoreau", firstName: "Thomas", lastName: "Moreau", company: "TechSolutions", mails: ["thomas.moreau@techsol.com"], groups: [{ name: "Sales" }] },
      { id: "crichard", firstName: "Claire", lastName: "Richard", company: "Ligoj", mails: ["claire.richard@ligoj.org"], groups: [{ name: "Management" }] },
      { id: "agarcia", firstName: "Antoine", lastName: "Garcia", company: "Ligoj", mails: ["antoine.garcia@ligoj.org"], groups: [{ name: "Engineering" }] }
    ];
    function O(T) {
      var W;
      const u = G.find((K) => K.id === T);
      u && ($.value.id = u.id, $.value.firstName = u.firstName, $.value.lastName = u.lastName, $.value.company = u.company, $.value.mail = ((W = u.mails) == null ? void 0 : W[0]) || "", P.value = u.groups || [], m.value = !!u.locked, M.value = !!u.isolated);
    }
    se(async () => {
      var T;
      if (B.value) {
        D.value = !0;
        const u = await w.get(`rest/service/id/user/${b.params.id}`);
        u && !u.code ? ($.value.id = u.id || "", $.value.firstName = u.firstName || "", $.value.lastName = u.lastName || "", $.value.company = u.company || "", $.value.mail = ((T = u.mails) == null ? void 0 : T[0]) || "", P.value = u.groups || [], m.value = !!u.locked, M.value = !!u.isolated) : (S.value = !0, v.clear(), O(b.params.id)), D.value = !1, A.setTitle(s("user.edit")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("user.title"), to: "/id/user" },
          { title: $.value.id || s("user.edit") }
        ]);
      } else {
        A.setTitle(s("user.new")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("user.title"), to: "/id/user" },
          { title: s("user.new") }
        ]);
        const u = await w.get("rest/service/id/user/admin");
        (!u || u.code) && (S.value = !0, v.clear());
      }
      p();
    });
    async function J() {
      const { valid: T } = await c.value.validate();
      if (!T) return;
      if (S.value) {
        v.push({ message: s("user.demoSave"), status: 0 });
        return;
      }
      L.value = !0;
      const u = {
        id: $.value.id,
        firstName: $.value.firstName,
        lastName: $.value.lastName,
        company: $.value.company,
        mail: $.value.mail
      };
      B.value ? await w.put("rest/service/id/user", u) : await w.post("rest/service/id/user", u), L.value = !1, y(), E.push("/id/user");
    }
    async function X() {
      if (S.value) {
        v.push({ message: s("user.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      x.value = !0, await w.del(`rest/service/id/user/${b.params.id}`), x.value = !1, V.value = !1, y(), E.push("/id/user");
    }
    function R(T) {
      I.value = T, F.value = !0;
    }
    async function Y() {
      if (S.value) {
        v.push({ message: s("user.demoAction"), status: 0 }), F.value = !1;
        return;
      }
      ee.value = !0;
      const T = $.value.id;
      await {
        lock: () => w.del(`rest/service/id/user/${T}/lock`),
        unlock: () => w.put(`rest/service/id/user/${T}/unlock`),
        isolate: () => w.del(`rest/service/id/user/${T}/isolate`),
        restore: () => w.put(`rest/service/id/user/${T}/restore`),
        resetPassword: () => w.put(`rest/service/id/user/${T}/reset`)
      }[I.value](), ee.value = !1, F.value = !1, I.value === "lock" && (m.value = !0), I.value === "unlock" && (m.value = !1), I.value === "isolate" && (M.value = !0), I.value === "restore" && (M.value = !1);
    }
    return (T, u) => {
      const W = i("v-alert"), K = i("v-skeleton-loader"), C = i("v-text-field"), N = i("v-form"), r = i("v-card-text"), j = i("v-icon"), ae = i("v-btn"), pe = i("v-spacer"), fe = i("v-card-actions"), H = i("v-card"), me = i("v-card-title"), Ce = i("v-dialog");
      return k(), ne("div", null, [
        te("div", Ie, [
          te("h1", qe, n(B.value ? l(s)("user.edit") : l(s)("user.new")), 1)
        ]),
        S.value ? (k(), h(W, {
          key: 0,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(s)("user.demoEdit")), 1)
          ]),
          _: 1
        })) : U("", !0),
        D.value ? (k(), h(K, {
          key: 1,
          type: "card, actions",
          "max-width": "700",
          class: "mb-4"
        })) : U("", !0),
        D.value ? U("", !0) : (k(), h(H, {
          key: 2,
          class: "edit-card"
        }, {
          default: t(() => [
            e(r, null, {
              default: t(() => [
                e(N, {
                  ref_key: "formRef",
                  ref: c,
                  onSubmit: oe(J, ["prevent"])
                }, {
                  default: t(() => [
                    e(C, {
                      modelValue: $.value.id,
                      "onUpdate:modelValue": u[0] || (u[0] = (Z) => $.value.id = Z),
                      label: l(s)("user.login"),
                      rules: [Q.required],
                      disabled: B.value,
                      hint: B.value ? "" : l(s)("user.loginHint"),
                      "persistent-hint": "",
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules", "disabled", "hint"]),
                    e(C, {
                      modelValue: $.value.firstName,
                      "onUpdate:modelValue": u[1] || (u[1] = (Z) => $.value.firstName = Z),
                      label: l(s)("user.firstName"),
                      rules: [Q.required],
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules"]),
                    e(C, {
                      modelValue: $.value.lastName,
                      "onUpdate:modelValue": u[2] || (u[2] = (Z) => $.value.lastName = Z),
                      label: l(s)("user.lastName"),
                      rules: [Q.required],
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules"]),
                    e(C, {
                      modelValue: $.value.company,
                      "onUpdate:modelValue": u[3] || (u[3] = (Z) => $.value.company = Z),
                      label: l(s)("user.company"),
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"]),
                    e(C, {
                      modelValue: $.value.mail,
                      "onUpdate:modelValue": u[4] || (u[4] = (Z) => $.value.mail = Z),
                      label: l(s)("user.email"),
                      type: "email",
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"]),
                    B.value ? (k(), h(C, {
                      key: 0,
                      "model-value": le.value,
                      label: l(s)("user.groups"),
                      variant: "outlined",
                      readonly: "",
                      class: "mb-2"
                    }, null, 8, ["model-value", "label"])) : U("", !0)
                  ]),
                  _: 1
                }, 512)
              ]),
              _: 1
            }),
            e(fe, null, {
              default: t(() => [
                B.value ? (k(), h(ae, {
                  key: 0,
                  color: "error",
                  variant: "tonal",
                  onClick: u[5] || (u[5] = (Z) => V.value = !0)
                }, {
                  default: t(() => [
                    e(j, { start: "" }, {
                      default: t(() => [...u[17] || (u[17] = [
                        a("mdi-delete", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.delete")), 1)
                  ]),
                  _: 1
                })) : U("", !0),
                e(pe),
                e(ae, {
                  variant: "text",
                  onClick: u[6] || (u[6] = (Z) => l(E).push("/id/user"))
                }, {
                  default: t(() => [
                    a(n(l(s)("common.cancel")), 1)
                  ]),
                  _: 1
                }),
                e(ae, {
                  color: "primary",
                  variant: "elevated",
                  loading: L.value,
                  onClick: J
                }, {
                  default: t(() => [
                    e(j, { start: "" }, {
                      default: t(() => [...u[18] || (u[18] = [
                        a("mdi-content-save", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.save")), 1)
                  ]),
                  _: 1
                }, 8, ["loading"])
              ]),
              _: 1
            })
          ]),
          _: 1
        })),
        B.value && !D.value ? (k(), h(H, {
          key: 3,
          class: "edit-card mt-4"
        }, {
          default: t(() => [
            e(me, { class: "text-h6" }, {
              default: t(() => [
                a(n(l(s)("user.actions")), 1)
              ]),
              _: 1
            }),
            e(r, null, {
              default: t(() => [
                te("div", We, [
                  m.value ? U("", !0) : (k(), h(ae, {
                    key: 0,
                    color: "warning",
                    variant: "tonal",
                    "prepend-icon": "mdi-lock",
                    onClick: u[7] || (u[7] = (Z) => R("lock"))
                  }, {
                    default: t(() => [
                      a(n(l(s)("user.lock")), 1)
                    ]),
                    _: 1
                  })),
                  m.value ? (k(), h(ae, {
                    key: 1,
                    color: "success",
                    variant: "tonal",
                    "prepend-icon": "mdi-lock-open-variant",
                    onClick: u[8] || (u[8] = (Z) => R("unlock"))
                  }, {
                    default: t(() => [
                      a(n(l(s)("user.unlock")), 1)
                    ]),
                    _: 1
                  })) : U("", !0),
                  M.value ? U("", !0) : (k(), h(ae, {
                    key: 2,
                    color: "error",
                    variant: "tonal",
                    "prepend-icon": "mdi-account-off",
                    onClick: u[9] || (u[9] = (Z) => R("isolate"))
                  }, {
                    default: t(() => [
                      a(n(l(s)("user.isolate")), 1)
                    ]),
                    _: 1
                  })),
                  M.value ? (k(), h(ae, {
                    key: 3,
                    color: "success",
                    variant: "tonal",
                    "prepend-icon": "mdi-account-check",
                    onClick: u[10] || (u[10] = (Z) => R("restore"))
                  }, {
                    default: t(() => [
                      a(n(l(s)("user.restore")), 1)
                    ]),
                    _: 1
                  })) : U("", !0),
                  e(ae, {
                    color: "info",
                    variant: "tonal",
                    "prepend-icon": "mdi-lock-reset",
                    onClick: u[11] || (u[11] = (Z) => R("resetPassword"))
                  }, {
                    default: t(() => [
                      a(n(l(s)("user.resetPassword")), 1)
                    ]),
                    _: 1
                  })
                ])
              ]),
              _: 1
            })
          ]),
          _: 1
        })) : U("", !0),
        e(Ce, {
          modelValue: V.value,
          "onUpdate:modelValue": u[13] || (u[13] = (Z) => V.value = Z),
          "max-width": "400"
        }, {
          default: t(() => [
            e(H, null, {
              default: t(() => [
                e(me, null, {
                  default: t(() => [
                    a(n(l(s)("user.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(r, null, {
                  default: t(() => [
                    a(n(l(s)("user.deleteConfirm", { id: $.value.id })), 1)
                  ]),
                  _: 1
                }),
                e(fe, null, {
                  default: t(() => [
                    e(pe),
                    e(ae, {
                      variant: "text",
                      onClick: u[12] || (u[12] = (Z) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(ae, {
                      color: "error",
                      variant: "elevated",
                      loading: x.value,
                      onClick: X
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.delete")), 1)
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
        e(Ce, {
          modelValue: l(d),
          "onUpdate:modelValue": u[14] || (u[14] = (Z) => be(d) ? d.value = Z : null),
          "max-width": "400"
        }, {
          default: t(() => [
            e(H, null, {
              default: t(() => [
                e(me, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedTitle")), 1)
                  ]),
                  _: 1
                }),
                e(r, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedMsg")), 1)
                  ]),
                  _: 1
                }),
                e(fe, null, {
                  default: t(() => [
                    e(pe),
                    e(ae, {
                      variant: "text",
                      onClick: l(_)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"]),
                    e(ae, {
                      color: "warning",
                      variant: "elevated",
                      onClick: l(f)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.discard")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"])
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })
          ]),
          _: 1
        }, 8, ["modelValue"]),
        e(Ce, {
          modelValue: F.value,
          "onUpdate:modelValue": u[16] || (u[16] = (Z) => F.value = Z),
          "max-width": "400"
        }, {
          default: t(() => [
            e(H, null, {
              default: t(() => [
                e(me, null, {
                  default: t(() => [
                    a(n(l(s)("user." + I.value)), 1)
                  ]),
                  _: 1
                }),
                e(r, null, {
                  default: t(() => [
                    a(n(l(s)("user." + I.value + "Confirm", { id: $.value.id })), 1)
                  ]),
                  _: 1
                }),
                e(fe, null, {
                  default: t(() => [
                    e(pe),
                    e(ae, {
                      variant: "text",
                      onClick: u[15] || (u[15] = (Z) => F.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(ae, {
                      color: "primary",
                      variant: "elevated",
                      loading: ee.value,
                      onClick: Y
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.confirm")), 1)
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
}, De = /* @__PURE__ */ ce(Fe, [["__scopeId", "data-v-0a7e5f18"]]), Ye = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, He = { class: "text-h4" }, Je = {
  __name: "GroupListView",
  setup(z) {
    const b = ve(), E = re(), w = ue(), A = _e(), o = de().t, c = Ve("service/id/group", { defaultSort: "name", demoData: [
      { name: "Engineering", scope: "Group", count: 4, locked: !1 },
      { name: "Marketing", scope: "Group", count: 1, locked: !1 },
      { name: "DevOps", scope: "Group", count: 2, locked: !1 },
      { name: "Management", scope: "Group", count: 2, locked: !1 },
      { name: "Sales", scope: "Group", count: 1, locked: !1 }
    ] }), D = g(25);
    let L = null;
    const x = g([]), V = g(!1), S = g(null), P = g(!1), m = g(!1);
    let M = {};
    const F = ie(() => [
      { title: o("common.name"), key: "name", sortable: !0 },
      { title: o("group.scope"), key: "scope", sortable: !1 },
      { title: o("group.members"), key: "count", sortable: !1, width: "100px" },
      { title: o("group.locked"), key: "locked", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]);
    function I(d) {
      M = d, c.load(d);
    }
    function ee() {
      clearTimeout(L), L = setTimeout(() => c.load({ page: 1, itemsPerPage: D.value }), 300);
    }
    function B(d) {
      S.value = d, V.value = !0;
    }
    async function le() {
      if (c.demoMode.value) {
        A.push({ message: o("group.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      P.value = !0, await w.del(`rest/service/id/group/${S.value.name}`), P.value = !1, V.value = !1, S.value = null, c.load(M);
    }
    function $() {
      m.value = !0;
    }
    async function q() {
      if (c.demoMode.value) {
        A.push({ message: o("group.demoDelete"), status: 0 }), m.value = !1;
        return;
      }
      P.value = !0;
      for (const d of x.value)
        await w.del(`rest/service/id/group/${d}`);
      P.value = !1, m.value = !1, x.value = [], c.load(M);
    }
    return se(() => {
      E.setTitle(o("group.title")), E.setBreadcrumbs([
        { title: o("nav.home"), to: "/" },
        { title: o("nav.identity") },
        { title: o("group.title") }
      ]);
    }), (d, f) => {
      const _ = i("v-spacer"), y = i("v-text-field"), p = i("v-btn"), Q = i("v-alert-title"), G = i("v-alert"), O = i("v-toolbar-title"), J = i("v-toolbar"), X = i("v-slide-y-transition"), R = i("v-skeleton-loader"), Y = i("v-icon"), T = i("v-data-table-server"), u = i("v-card-title"), W = i("v-card-text"), K = i("v-card-actions"), C = i("v-card"), N = i("v-dialog");
      return k(), ne("div", null, [
        te("div", Ye, [
          te("h1", He, n(l(o)("group.title")), 1),
          e(_),
          e(y, {
            modelValue: l(c).search.value,
            "onUpdate:modelValue": [
              f[0] || (f[0] = (r) => l(c).search.value = r),
              ee
            ],
            "prepend-inner-icon": "mdi-magnify",
            label: l(o)("common.search"),
            variant: "outlined",
            density: "compact",
            "hide-details": "",
            class: "search-field"
          }, null, 8, ["modelValue", "label"]),
          e(p, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: f[1] || (f[1] = (r) => l(b).push("/id/group/new"))
          }, {
            default: t(() => [
              a(n(l(o)("group.new")), 1)
            ]),
            _: 1
          })
        ]),
        l(c).error.value ? (k(), h(G, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            e(Q, null, {
              default: t(() => [
                a(n(l(o)("user.noProvider")), 1)
              ]),
              _: 1
            }),
            a(" " + n(l(c).error.value === "internal" ? l(o)("group.noProvider") : l(c).error.value), 1)
          ]),
          _: 1
        })) : U("", !0),
        l(c).demoMode.value ? (k(), h(G, {
          key: 1,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(o)("user.demoMode")), 1)
          ]),
          _: 1
        })) : U("", !0),
        e(X, null, {
          default: t(() => [
            x.value.length ? (k(), h(J, {
              key: 0,
              density: "compact",
              color: "primary",
              rounded: "",
              class: "mb-4"
            }, {
              default: t(() => [
                e(O, null, {
                  default: t(() => [
                    a(n(x.value.length) + " " + n(l(o)("common.selected")), 1)
                  ]),
                  _: 1
                }),
                e(_),
                e(p, {
                  variant: "elevated",
                  color: "error",
                  "prepend-icon": "mdi-delete",
                  onClick: $
                }, {
                  default: t(() => [
                    a(n(l(o)("common.delete")), 1)
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })) : U("", !0)
          ]),
          _: 1
        }),
        l(c).loading.value && l(c).items.value.length === 0 ? (k(), h(R, {
          key: 2,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : U("", !0),
        l(c).error.value ? U("", !0) : ye((k(), h(T, {
          key: 3,
          modelValue: x.value,
          "onUpdate:modelValue": f[2] || (f[2] = (r) => x.value = r),
          "items-per-page": D.value,
          "onUpdate:itemsPerPage": f[3] || (f[3] = (r) => D.value = r),
          headers: F.value,
          items: l(c).items.value,
          "items-length": l(c).totalItems.value,
          loading: l(c).loading.value,
          "item-value": "name",
          "show-select": "",
          hover: "",
          "onUpdate:options": I,
          "onClick:row": f[4] || (f[4] = (r, { item: j }) => l(b).push("/id/group/" + j.name))
        }, {
          "item.locked": t(({ item: r }) => [
            r.locked ? (k(), h(Y, {
              key: 0,
              color: "error",
              size: "small"
            }, {
              default: t(() => [...f[9] || (f[9] = [
                a("mdi-lock", -1)
              ])]),
              _: 1
            })) : U("", !0)
          ]),
          "item.actions": t(({ item: r }) => [
            e(p, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: oe((j) => l(b).push("/id/group/" + r.name), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...f[10] || (f[10] = [
                    a("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            e(p, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: oe((j) => B(r), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...f[11] || (f[11] = [
                    a("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"])
          ]),
          _: 1
        }, 8, ["modelValue", "items-per-page", "headers", "items", "items-length", "loading"])), [
          [ke, l(c).items.value.length > 0 || !l(c).loading.value]
        ]),
        e(N, {
          modelValue: V.value,
          "onUpdate:modelValue": f[6] || (f[6] = (r) => V.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("group.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => {
                    var r;
                    return [
                      a(n(l(o)("group.deleteConfirm", { name: (r = S.value) == null ? void 0 : r.name })), 1)
                    ];
                  }),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(_),
                    e(p, {
                      variant: "text",
                      onClick: f[5] || (f[5] = (r) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(p, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: le
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
        e(N, {
          modelValue: m.value,
          "onUpdate:modelValue": f[8] || (f[8] = (r) => m.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteConfirm", { count: x.value.length })), 1)
                  ]),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(_),
                    e(p, {
                      variant: "text",
                      onClick: f[7] || (f[7] = (r) => m.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(p, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: q
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
}, Ke = /* @__PURE__ */ ce(Je, [["__scopeId", "data-v-5420611e"]]), Qe = { class: "d-flex align-center mb-4" }, Xe = { class: "text-h4" }, Ze = {
  __name: "GroupEditView",
  setup(z) {
    const b = we(), E = ve(), w = ue(), A = re(), v = _e(), s = de().t, c = g(null), D = g(!1), L = g(!1), x = g(!1), V = g(!1), S = g(!1), P = g([]), m = ie(() => b.params.id && b.params.id !== "new"), M = g({
      name: "",
      scope: "",
      parent: ""
    }), { isDirty: F, showGuardDialog: I, confirmLeave: ee, cancelLeave: B, markClean: le, init: $ } = xe(M), q = {
      required: (y) => !!y || s("common.required")
    }, d = [
      { name: "Engineering", scope: "Group" },
      { name: "Marketing", scope: "Group" },
      { name: "DevOps", scope: "Group" },
      { name: "Management", scope: "Group" },
      { name: "Sales", scope: "Group" }
    ];
    se(async () => {
      const y = await w.get("rest/service/id/group");
      if (y && Array.isArray(y) ? P.value = y.map((p) => p.name || p.id || p).filter(Boolean) : y != null && y.data && Array.isArray(y.data) ? P.value = y.data.map((p) => p.name || p.id || p).filter(Boolean) : P.value = d.map((p) => p.name), m.value) {
        D.value = !0;
        const p = await w.get(`rest/service/id/group/${b.params.id}`);
        if (p && !p.code)
          M.value.name = p.name || "", M.value.scope = p.scope || "", M.value.parent = p.parent || "";
        else {
          S.value = !0, v.clear();
          const Q = d.find((G) => G.name === b.params.id);
          Q && (M.value.name = Q.name, M.value.scope = Q.scope, M.value.parent = "");
        }
        D.value = !1, A.setTitle(s("group.edit")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("group.title"), to: "/id/group" },
          { title: M.value.name || s("group.edit") }
        ]);
      } else {
        A.setTitle(s("group.new")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("group.title"), to: "/id/group" },
          { title: s("group.new") }
        ]);
        const p = await w.get("rest/service/id/group/Engineering");
        (!p || p.code) && (S.value = !0, v.clear());
      }
      $();
    });
    async function f() {
      const { valid: y } = await c.value.validate();
      if (!y) return;
      if (S.value) {
        v.push({ message: s("group.demoSave"), status: 0 });
        return;
      }
      L.value = !0;
      const p = { name: M.value.name, scope: M.value.scope, parent: M.value.parent || null };
      m.value ? await w.put("rest/service/id/group", p) : await w.post("rest/service/id/group", p), L.value = !1, le(), E.push("/id/group");
    }
    async function _() {
      if (S.value) {
        v.push({ message: s("group.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      x.value = !0, await w.del(`rest/service/id/group/${b.params.id}`), x.value = !1, V.value = !1, le(), E.push("/id/group");
    }
    return (y, p) => {
      const Q = i("v-alert"), G = i("v-skeleton-loader"), O = i("v-text-field"), J = i("v-autocomplete"), X = i("v-form"), R = i("v-card-text"), Y = i("v-icon"), T = i("v-btn"), u = i("v-spacer"), W = i("v-card-actions"), K = i("v-card"), C = i("v-card-title"), N = i("v-dialog");
      return k(), ne("div", null, [
        te("div", Qe, [
          te("h1", Xe, n(m.value ? l(s)("group.edit") : l(s)("group.new")), 1)
        ]),
        S.value ? (k(), h(Q, {
          key: 0,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(s)("group.demoEdit")), 1)
          ]),
          _: 1
        })) : U("", !0),
        D.value ? (k(), h(G, {
          key: 1,
          type: "card, actions",
          "max-width": "700",
          class: "mb-4"
        })) : U("", !0),
        D.value ? U("", !0) : (k(), h(K, {
          key: 2,
          class: "edit-card"
        }, {
          default: t(() => [
            e(R, null, {
              default: t(() => [
                e(X, {
                  ref_key: "formRef",
                  ref: c,
                  onSubmit: oe(f, ["prevent"])
                }, {
                  default: t(() => [
                    e(O, {
                      modelValue: M.value.name,
                      "onUpdate:modelValue": p[0] || (p[0] = (r) => M.value.name = r),
                      label: l(s)("common.name"),
                      rules: [q.required],
                      disabled: m.value,
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules", "disabled"]),
                    e(O, {
                      modelValue: M.value.scope,
                      "onUpdate:modelValue": p[1] || (p[1] = (r) => M.value.scope = r),
                      label: l(s)("group.scope"),
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"]),
                    e(J, {
                      modelValue: M.value.parent,
                      "onUpdate:modelValue": p[2] || (p[2] = (r) => M.value.parent = r),
                      label: l(s)("group.parent"),
                      items: P.value,
                      hint: l(s)("group.parentHint"),
                      "persistent-hint": "",
                      clearable: "",
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "items", "hint"])
                  ]),
                  _: 1
                }, 512)
              ]),
              _: 1
            }),
            e(W, null, {
              default: t(() => [
                m.value ? (k(), h(T, {
                  key: 0,
                  color: "error",
                  variant: "tonal",
                  onClick: p[3] || (p[3] = (r) => V.value = !0)
                }, {
                  default: t(() => [
                    e(Y, { start: "" }, {
                      default: t(() => [...p[8] || (p[8] = [
                        a("mdi-delete", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.delete")), 1)
                  ]),
                  _: 1
                })) : U("", !0),
                e(u),
                e(T, {
                  variant: "text",
                  onClick: p[4] || (p[4] = (r) => l(E).push("/id/group"))
                }, {
                  default: t(() => [
                    a(n(l(s)("common.cancel")), 1)
                  ]),
                  _: 1
                }),
                e(T, {
                  color: "primary",
                  variant: "elevated",
                  loading: L.value,
                  onClick: f
                }, {
                  default: t(() => [
                    e(Y, { start: "" }, {
                      default: t(() => [...p[9] || (p[9] = [
                        a("mdi-content-save", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.save")), 1)
                  ]),
                  _: 1
                }, 8, ["loading"])
              ]),
              _: 1
            })
          ]),
          _: 1
        })),
        e(N, {
          modelValue: V.value,
          "onUpdate:modelValue": p[6] || (p[6] = (r) => V.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(K, null, {
              default: t(() => [
                e(C, null, {
                  default: t(() => [
                    a(n(l(s)("group.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(R, null, {
                  default: t(() => [
                    a(n(l(s)("group.deleteConfirm", { name: M.value.name })), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    e(u),
                    e(T, {
                      variant: "text",
                      onClick: p[5] || (p[5] = (r) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(T, {
                      color: "error",
                      variant: "elevated",
                      loading: x.value,
                      onClick: _
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.delete")), 1)
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
        e(N, {
          modelValue: l(I),
          "onUpdate:modelValue": p[7] || (p[7] = (r) => be(I) ? I.value = r : null),
          "max-width": "400"
        }, {
          default: t(() => [
            e(K, null, {
              default: t(() => [
                e(C, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedTitle")), 1)
                  ]),
                  _: 1
                }),
                e(R, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedMsg")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    e(u),
                    e(T, {
                      variant: "text",
                      onClick: l(B)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"]),
                    e(T, {
                      color: "warning",
                      variant: "elevated",
                      onClick: l(ee)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.discard")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"])
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
}, $e = /* @__PURE__ */ ce(Ze, [["__scopeId", "data-v-5c142fd4"]]), et = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, tt = { class: "text-h4" }, lt = {
  __name: "CompanyListView",
  setup(z) {
    const b = ve(), E = re(), w = ue(), A = _e(), o = de().t, c = Ve("service/id/company", { defaultSort: "name", demoData: [
      { name: "Ligoj", scope: "Company", count: 4, locked: !1 },
      { name: "AcmeCorp", scope: "Company", count: 2, locked: !1 },
      { name: "TechSolutions", scope: "Company", count: 2, locked: !1 }
    ] }), D = g(25);
    let L = null;
    const x = g([]), V = g(!1), S = g(null), P = g(!1), m = g(!1);
    let M = {};
    const F = ie(() => [
      { title: o("common.name"), key: "name", sortable: !0 },
      { title: o("group.scope"), key: "scope", sortable: !1 },
      { title: o("group.members"), key: "count", sortable: !1, width: "100px" },
      { title: o("group.locked"), key: "locked", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]);
    function I(d) {
      M = d, c.load(d);
    }
    function ee() {
      clearTimeout(L), L = setTimeout(() => c.load({ page: 1, itemsPerPage: D.value }), 300);
    }
    function B(d) {
      S.value = d, V.value = !0;
    }
    async function le() {
      if (c.demoMode.value) {
        A.push({ message: o("company.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      P.value = !0, await w.del(`rest/service/id/company/${S.value.name}`), P.value = !1, V.value = !1, S.value = null, c.load(M);
    }
    function $() {
      m.value = !0;
    }
    async function q() {
      if (c.demoMode.value) {
        A.push({ message: o("company.demoDelete"), status: 0 }), m.value = !1;
        return;
      }
      P.value = !0;
      for (const d of x.value)
        await w.del(`rest/service/id/company/${d}`);
      P.value = !1, m.value = !1, x.value = [], c.load(M);
    }
    return se(() => {
      E.setTitle(o("company.title")), E.setBreadcrumbs([
        { title: o("nav.home"), to: "/" },
        { title: o("nav.identity") },
        { title: o("company.title") }
      ]);
    }), (d, f) => {
      const _ = i("v-spacer"), y = i("v-text-field"), p = i("v-btn"), Q = i("v-alert-title"), G = i("v-alert"), O = i("v-toolbar-title"), J = i("v-toolbar"), X = i("v-slide-y-transition"), R = i("v-skeleton-loader"), Y = i("v-icon"), T = i("v-data-table-server"), u = i("v-card-title"), W = i("v-card-text"), K = i("v-card-actions"), C = i("v-card"), N = i("v-dialog");
      return k(), ne("div", null, [
        te("div", et, [
          te("h1", tt, n(l(o)("company.title")), 1),
          e(_),
          e(y, {
            modelValue: l(c).search.value,
            "onUpdate:modelValue": [
              f[0] || (f[0] = (r) => l(c).search.value = r),
              ee
            ],
            "prepend-inner-icon": "mdi-magnify",
            label: l(o)("common.search"),
            variant: "outlined",
            density: "compact",
            "hide-details": "",
            class: "search-field"
          }, null, 8, ["modelValue", "label"]),
          e(p, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: f[1] || (f[1] = (r) => l(b).push("/id/company/new"))
          }, {
            default: t(() => [
              a(n(l(o)("company.new")), 1)
            ]),
            _: 1
          })
        ]),
        l(c).error.value ? (k(), h(G, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            e(Q, null, {
              default: t(() => [
                a(n(l(o)("user.noProvider")), 1)
              ]),
              _: 1
            }),
            a(" " + n(l(c).error.value === "internal" ? l(o)("company.noProvider") : l(c).error.value), 1)
          ]),
          _: 1
        })) : U("", !0),
        l(c).demoMode.value ? (k(), h(G, {
          key: 1,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(o)("user.demoMode")), 1)
          ]),
          _: 1
        })) : U("", !0),
        e(X, null, {
          default: t(() => [
            x.value.length ? (k(), h(J, {
              key: 0,
              density: "compact",
              color: "primary",
              rounded: "",
              class: "mb-4"
            }, {
              default: t(() => [
                e(O, null, {
                  default: t(() => [
                    a(n(x.value.length) + " " + n(l(o)("common.selected")), 1)
                  ]),
                  _: 1
                }),
                e(_),
                e(p, {
                  variant: "elevated",
                  color: "error",
                  "prepend-icon": "mdi-delete",
                  onClick: $
                }, {
                  default: t(() => [
                    a(n(l(o)("common.delete")), 1)
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })) : U("", !0)
          ]),
          _: 1
        }),
        l(c).loading.value && l(c).items.value.length === 0 ? (k(), h(R, {
          key: 2,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : U("", !0),
        l(c).error.value ? U("", !0) : ye((k(), h(T, {
          key: 3,
          modelValue: x.value,
          "onUpdate:modelValue": f[2] || (f[2] = (r) => x.value = r),
          "items-per-page": D.value,
          "onUpdate:itemsPerPage": f[3] || (f[3] = (r) => D.value = r),
          headers: F.value,
          items: l(c).items.value,
          "items-length": l(c).totalItems.value,
          loading: l(c).loading.value,
          "item-value": "name",
          "show-select": "",
          hover: "",
          "onUpdate:options": I,
          "onClick:row": f[4] || (f[4] = (r, { item: j }) => l(b).push("/id/company/" + j.name))
        }, {
          "item.locked": t(({ item: r }) => [
            r.locked ? (k(), h(Y, {
              key: 0,
              color: "error",
              size: "small"
            }, {
              default: t(() => [...f[9] || (f[9] = [
                a("mdi-lock", -1)
              ])]),
              _: 1
            })) : U("", !0)
          ]),
          "item.actions": t(({ item: r }) => [
            e(p, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: oe((j) => l(b).push("/id/company/" + r.name), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...f[10] || (f[10] = [
                    a("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            e(p, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: oe((j) => B(r), ["stop"])
            }, {
              default: t(() => [
                e(Y, { size: "small" }, {
                  default: t(() => [...f[11] || (f[11] = [
                    a("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"])
          ]),
          _: 1
        }, 8, ["modelValue", "items-per-page", "headers", "items", "items-length", "loading"])), [
          [ke, l(c).items.value.length > 0 || !l(c).loading.value]
        ]),
        e(N, {
          modelValue: V.value,
          "onUpdate:modelValue": f[6] || (f[6] = (r) => V.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("company.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => {
                    var r;
                    return [
                      a(n(l(o)("company.deleteConfirm", { name: (r = S.value) == null ? void 0 : r.name })), 1)
                    ];
                  }),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(_),
                    e(p, {
                      variant: "text",
                      onClick: f[5] || (f[5] = (r) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(p, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: le
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
        e(N, {
          modelValue: m.value,
          "onUpdate:modelValue": f[8] || (f[8] = (r) => m.value = r),
          "max-width": "400"
        }, {
          default: t(() => [
            e(C, null, {
              default: t(() => [
                e(u, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    a(n(l(o)("common.bulkDeleteConfirm", { count: x.value.length })), 1)
                  ]),
                  _: 1
                }),
                e(K, null, {
                  default: t(() => [
                    e(_),
                    e(p, {
                      variant: "text",
                      onClick: f[7] || (f[7] = (r) => m.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(p, {
                      color: "error",
                      variant: "elevated",
                      loading: P.value,
                      onClick: q
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
}, at = /* @__PURE__ */ ce(lt, [["__scopeId", "data-v-ed5a1b7e"]]), ot = { class: "d-flex align-center mb-4" }, nt = { class: "text-h4" }, it = {
  __name: "CompanyEditView",
  setup(z) {
    const b = we(), E = ve(), w = ue(), A = re(), v = _e(), s = de().t, c = g(null), D = g(!1), L = g(!1), x = g(!1), V = g(!1), S = g(!1), P = ie(() => b.params.id && b.params.id !== "new"), m = g({
      name: "",
      scope: ""
    }), { isDirty: M, showGuardDialog: F, confirmLeave: I, cancelLeave: ee, markClean: B, init: le } = xe(m), $ = {
      required: (_) => !!_ || s("common.required")
    }, q = [
      { name: "Ligoj", scope: "Company" },
      { name: "AcmeCorp", scope: "Company" },
      { name: "TechSolutions", scope: "Company" }
    ];
    se(async () => {
      if (P.value) {
        D.value = !0;
        const _ = await w.get(`rest/service/id/company/${b.params.id}`);
        if (_ && !_.code)
          m.value.name = _.name || "", m.value.scope = _.scope || "";
        else {
          S.value = !0, v.clear();
          const y = q.find((p) => p.name === b.params.id);
          y && (m.value.name = y.name, m.value.scope = y.scope);
        }
        D.value = !1, A.setTitle(s("company.edit")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("company.title"), to: "/id/company" },
          { title: m.value.name || s("company.edit") }
        ]);
      } else {
        A.setTitle(s("company.new")), A.setBreadcrumbs([
          { title: s("nav.home"), to: "/" },
          { title: s("nav.identity") },
          { title: s("company.title"), to: "/id/company" },
          { title: s("company.new") }
        ]);
        const _ = await w.get("rest/service/id/company/Ligoj");
        (!_ || _.code) && (S.value = !0, v.clear());
      }
      le();
    });
    async function d() {
      const { valid: _ } = await c.value.validate();
      if (!_) return;
      if (S.value) {
        v.push({ message: s("company.demoSave"), status: 0 });
        return;
      }
      L.value = !0;
      const y = { name: m.value.name, scope: m.value.scope };
      P.value ? await w.put("rest/service/id/company", y) : await w.post("rest/service/id/company", y), L.value = !1, B(), E.push("/id/company");
    }
    async function f() {
      if (S.value) {
        v.push({ message: s("company.demoDelete"), status: 0 }), V.value = !1;
        return;
      }
      x.value = !0, await w.del(`rest/service/id/company/${b.params.id}`), x.value = !1, V.value = !1, B(), E.push("/id/company");
    }
    return (_, y) => {
      const p = i("v-alert"), Q = i("v-skeleton-loader"), G = i("v-text-field"), O = i("v-form"), J = i("v-card-text"), X = i("v-icon"), R = i("v-btn"), Y = i("v-spacer"), T = i("v-card-actions"), u = i("v-card"), W = i("v-card-title"), K = i("v-dialog");
      return k(), ne("div", null, [
        te("div", ot, [
          te("h1", nt, n(P.value ? l(s)("company.edit") : l(s)("company.new")), 1)
        ]),
        S.value ? (k(), h(p, {
          key: 0,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(s)("company.demoEdit")), 1)
          ]),
          _: 1
        })) : U("", !0),
        D.value ? (k(), h(Q, {
          key: 1,
          type: "card, actions",
          "max-width": "700",
          class: "mb-4"
        })) : U("", !0),
        D.value ? U("", !0) : (k(), h(u, {
          key: 2,
          class: "edit-card"
        }, {
          default: t(() => [
            e(J, null, {
              default: t(() => [
                e(O, {
                  ref_key: "formRef",
                  ref: c,
                  onSubmit: oe(d, ["prevent"])
                }, {
                  default: t(() => [
                    e(G, {
                      modelValue: m.value.name,
                      "onUpdate:modelValue": y[0] || (y[0] = (C) => m.value.name = C),
                      label: l(s)("common.name"),
                      rules: [$.required],
                      disabled: P.value,
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules", "disabled"]),
                    e(G, {
                      modelValue: m.value.scope,
                      "onUpdate:modelValue": y[1] || (y[1] = (C) => m.value.scope = C),
                      label: l(s)("group.scope"),
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"])
                  ]),
                  _: 1
                }, 512)
              ]),
              _: 1
            }),
            e(T, null, {
              default: t(() => [
                P.value ? (k(), h(R, {
                  key: 0,
                  color: "error",
                  variant: "tonal",
                  onClick: y[2] || (y[2] = (C) => V.value = !0)
                }, {
                  default: t(() => [
                    e(X, { start: "" }, {
                      default: t(() => [...y[7] || (y[7] = [
                        a("mdi-delete", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.delete")), 1)
                  ]),
                  _: 1
                })) : U("", !0),
                e(Y),
                e(R, {
                  variant: "text",
                  onClick: y[3] || (y[3] = (C) => l(E).push("/id/company"))
                }, {
                  default: t(() => [
                    a(n(l(s)("common.cancel")), 1)
                  ]),
                  _: 1
                }),
                e(R, {
                  color: "primary",
                  variant: "elevated",
                  loading: L.value,
                  onClick: d
                }, {
                  default: t(() => [
                    e(X, { start: "" }, {
                      default: t(() => [...y[8] || (y[8] = [
                        a("mdi-content-save", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(s)("common.save")), 1)
                  ]),
                  _: 1
                }, 8, ["loading"])
              ]),
              _: 1
            })
          ]),
          _: 1
        })),
        e(K, {
          modelValue: V.value,
          "onUpdate:modelValue": y[5] || (y[5] = (C) => V.value = C),
          "max-width": "400"
        }, {
          default: t(() => [
            e(u, null, {
              default: t(() => [
                e(W, null, {
                  default: t(() => [
                    a(n(l(s)("company.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(J, null, {
                  default: t(() => [
                    a(n(l(s)("company.deleteConfirm", { name: m.value.name })), 1)
                  ]),
                  _: 1
                }),
                e(T, null, {
                  default: t(() => [
                    e(Y),
                    e(R, {
                      variant: "text",
                      onClick: y[4] || (y[4] = (C) => V.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(R, {
                      color: "error",
                      variant: "elevated",
                      loading: x.value,
                      onClick: f
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.delete")), 1)
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
        e(K, {
          modelValue: l(F),
          "onUpdate:modelValue": y[6] || (y[6] = (C) => be(F) ? F.value = C : null),
          "max-width": "400"
        }, {
          default: t(() => [
            e(u, null, {
              default: t(() => [
                e(W, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedTitle")), 1)
                  ]),
                  _: 1
                }),
                e(J, null, {
                  default: t(() => [
                    a(n(l(s)("common.unsavedMsg")), 1)
                  ]),
                  _: 1
                }),
                e(T, null, {
                  default: t(() => [
                    e(Y),
                    e(R, {
                      variant: "text",
                      onClick: l(ee)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.cancel")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"]),
                    e(R, {
                      color: "warning",
                      variant: "elevated",
                      onClick: l(I)
                    }, {
                      default: t(() => [
                        a(n(l(s)("common.discard")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"])
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
}, Ue = /* @__PURE__ */ ce(it, [["__scopeId", "data-v-9f2567ab"]]), st = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, rt = { class: "text-h4" }, ut = {
  __name: "DelegateListView",
  setup(z) {
    const b = ve(), E = re(), w = ue(), v = de().t, o = Ve("security/delegate", { defaultSort: "receiver" }), s = g(25);
    let c = null;
    const D = g([]), L = g(!1), x = g(null), V = g(!1), S = g(!1);
    let P = {};
    const m = ie(() => [
      { title: v("delegate.receiver"), key: "receiver", sortable: !0 },
      { title: v("delegate.type"), key: "type", sortable: !1, width: "120px" },
      { title: v("delegate.resource"), key: "name", sortable: !1 },
      { title: v("delegate.admin"), key: "canAdmin", sortable: !1, width: "80px" },
      { title: v("delegate.write"), key: "canWrite", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]);
    function M(q) {
      return { USER: "blue", GROUP: "teal", COMPANY: "indigo", TREE: "orange" }[q] || "grey";
    }
    function F(q) {
      P = q, o.load(q);
    }
    function I() {
      clearTimeout(c), c = setTimeout(() => o.load({ page: 1, itemsPerPage: s.value }), 300);
    }
    function ee(q) {
      x.value = q, L.value = !0;
    }
    async function B() {
      V.value = !0, await w.del(`rest/security/delegate/${x.value.id}`), V.value = !1, L.value = !1, x.value = null, o.load(P);
    }
    function le() {
      S.value = !0;
    }
    async function $() {
      V.value = !0;
      for (const q of D.value)
        await w.del(`rest/security/delegate/${q}`);
      V.value = !1, S.value = !1, D.value = [], o.load(P);
    }
    return se(() => {
      E.setTitle(v("delegate.title")), E.setBreadcrumbs([
        { title: v("nav.home"), to: "/" },
        { title: v("nav.identity") },
        { title: v("delegate.title") }
      ]);
    }), (q, d) => {
      const f = i("v-spacer"), _ = i("v-text-field"), y = i("v-btn"), p = i("v-alert"), Q = i("v-toolbar-title"), G = i("v-toolbar"), O = i("v-slide-y-transition"), J = i("v-skeleton-loader"), X = i("v-chip"), R = i("v-icon"), Y = i("v-data-table-server"), T = i("v-card-title"), u = i("v-card-text"), W = i("v-card-actions"), K = i("v-card"), C = i("v-dialog");
      return k(), ne("div", null, [
        te("div", st, [
          te("h1", rt, n(l(v)("delegate.title")), 1),
          e(f),
          e(_, {
            modelValue: l(o).search.value,
            "onUpdate:modelValue": [
              d[0] || (d[0] = (N) => l(o).search.value = N),
              I
            ],
            "prepend-inner-icon": "mdi-magnify",
            label: l(v)("common.search"),
            variant: "outlined",
            density: "compact",
            "hide-details": "",
            class: "search-field"
          }, null, 8, ["modelValue", "label"]),
          e(y, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: d[1] || (d[1] = (N) => l(b).push("/id/delegate/new"))
          }, {
            default: t(() => [
              a(n(l(v)("delegate.new")), 1)
            ]),
            _: 1
          })
        ]),
        l(o).error.value ? (k(), h(p, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(o).error.value), 1)
          ]),
          _: 1
        })) : U("", !0),
        e(O, null, {
          default: t(() => [
            D.value.length ? (k(), h(G, {
              key: 0,
              density: "compact",
              color: "primary",
              rounded: "",
              class: "mb-4"
            }, {
              default: t(() => [
                e(Q, null, {
                  default: t(() => [
                    a(n(D.value.length) + " " + n(l(v)("common.selected")), 1)
                  ]),
                  _: 1
                }),
                e(f),
                e(y, {
                  variant: "elevated",
                  color: "error",
                  "prepend-icon": "mdi-delete",
                  onClick: le
                }, {
                  default: t(() => [
                    a(n(l(v)("common.delete")), 1)
                  ]),
                  _: 1
                })
              ]),
              _: 1
            })) : U("", !0)
          ]),
          _: 1
        }),
        l(o).loading.value && l(o).items.value.length === 0 ? (k(), h(J, {
          key: 1,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : U("", !0),
        l(o).error.value ? U("", !0) : ye((k(), h(Y, {
          key: 2,
          modelValue: D.value,
          "onUpdate:modelValue": d[2] || (d[2] = (N) => D.value = N),
          "items-per-page": s.value,
          "onUpdate:itemsPerPage": d[3] || (d[3] = (N) => s.value = N),
          headers: m.value,
          items: l(o).items.value,
          "items-length": l(o).totalItems.value,
          loading: l(o).loading.value,
          "item-value": "id",
          "show-select": "",
          hover: "",
          "onUpdate:options": F,
          "onClick:row": d[4] || (d[4] = (N, { item: r }) => l(b).push("/id/delegate/" + r.id))
        }, {
          "item.receiver": t(({ item: N }) => {
            var r, j;
            return [
              a(n(((r = N.receiver) == null ? void 0 : r.name) || ((j = N.receiver) == null ? void 0 : j.id) || N.name || "-"), 1)
            ];
          }),
          "item.type": t(({ item: N }) => [
            e(X, {
              size: "small",
              color: M(N.type || N.receiverType)
            }, {
              default: t(() => [
                a(n(N.type || N.receiverType || "-"), 1)
              ]),
              _: 2
            }, 1032, ["color"])
          ]),
          "item.canAdmin": t(({ item: N }) => [
            N.canAdmin ? (k(), h(R, {
              key: 0,
              color: "success",
              size: "small"
            }, {
              default: t(() => [...d[9] || (d[9] = [
                a("mdi-check", -1)
              ])]),
              _: 1
            })) : U("", !0)
          ]),
          "item.canWrite": t(({ item: N }) => [
            N.canWrite ? (k(), h(R, {
              key: 0,
              color: "success",
              size: "small"
            }, {
              default: t(() => [...d[10] || (d[10] = [
                a("mdi-check", -1)
              ])]),
              _: 1
            })) : U("", !0)
          ]),
          "item.actions": t(({ item: N }) => [
            e(y, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: oe((r) => l(b).push("/id/delegate/" + N.id), ["stop"])
            }, {
              default: t(() => [
                e(R, { size: "small" }, {
                  default: t(() => [...d[11] || (d[11] = [
                    a("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            e(y, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: oe((r) => ee(N), ["stop"])
            }, {
              default: t(() => [
                e(R, { size: "small" }, {
                  default: t(() => [...d[12] || (d[12] = [
                    a("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"])
          ]),
          _: 1
        }, 8, ["modelValue", "items-per-page", "headers", "items", "items-length", "loading"])), [
          [ke, l(o).items.value.length > 0 || !l(o).loading.value]
        ]),
        !l(o).loading.value && !l(o).error.value && l(o).totalItems.value === 0 ? (k(), h(p, {
          key: 3,
          type: "info",
          variant: "tonal",
          class: "mt-4"
        }, {
          default: t(() => [
            a(n(l(v)("delegate.empty")), 1)
          ]),
          _: 1
        })) : U("", !0),
        e(C, {
          modelValue: L.value,
          "onUpdate:modelValue": d[6] || (d[6] = (N) => L.value = N),
          "max-width": "400"
        }, {
          default: t(() => [
            e(K, null, {
              default: t(() => [
                e(T, null, {
                  default: t(() => [
                    a(n(l(v)("delegate.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(u, null, {
                  default: t(() => {
                    var N, r, j, ae;
                    return [
                      a(n(l(v)("delegate.deleteConfirm", { name: ((r = (N = x.value) == null ? void 0 : N.receiver) == null ? void 0 : r.name) || ((j = x.value) == null ? void 0 : j.name) || ((ae = x.value) == null ? void 0 : ae.id) })), 1)
                    ];
                  }),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    e(f),
                    e(y, {
                      variant: "text",
                      onClick: d[5] || (d[5] = (N) => L.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(y, {
                      color: "error",
                      variant: "elevated",
                      loading: V.value,
                      onClick: B
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.delete")), 1)
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
        e(C, {
          modelValue: S.value,
          "onUpdate:modelValue": d[8] || (d[8] = (N) => S.value = N),
          "max-width": "400"
        }, {
          default: t(() => [
            e(K, null, {
              default: t(() => [
                e(T, null, {
                  default: t(() => [
                    a(n(l(v)("common.bulkDeleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(u, null, {
                  default: t(() => [
                    a(n(l(v)("common.bulkDeleteConfirm", { count: D.value.length })), 1)
                  ]),
                  _: 1
                }),
                e(W, null, {
                  default: t(() => [
                    e(f),
                    e(y, {
                      variant: "text",
                      onClick: d[7] || (d[7] = (N) => S.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(y, {
                      color: "error",
                      variant: "elevated",
                      loading: V.value,
                      onClick: $
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.delete")), 1)
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
}, dt = /* @__PURE__ */ ce(ut, [["__scopeId", "data-v-e3d41033"]]), ct = { class: "d-flex align-center mb-4" }, mt = { class: "text-h4" }, vt = {
  __name: "DelegateEditView",
  setup(z) {
    const b = we(), E = ve(), w = ue(), A = re(), o = de().t, s = g(null), c = g(!1), D = g(!1), L = g(!1), x = g(!1), V = ie(() => !!b.params.id), S = ["USER", "GROUP", "COMPANY"], P = ["USER", "GROUP", "COMPANY", "TREE"], m = g({
      receiver: "",
      receiverType: "USER",
      name: "",
      type: "GROUP",
      canAdmin: !1,
      canWrite: !1
    }), { isDirty: M, showGuardDialog: F, confirmLeave: I, cancelLeave: ee, markClean: B, init: le } = xe(m), $ = {
      required: (f) => !!f || o("common.required")
    };
    se(async () => {
      var f;
      if (V.value) {
        c.value = !0;
        const _ = await w.get(`rest/security/delegate/${b.params.id}`);
        _ && (m.value.receiver = ((f = _.receiver) == null ? void 0 : f.id) || _.receiver || "", m.value.receiverType = _.receiverType || "USER", m.value.name = _.name || "", m.value.type = _.type || "GROUP", m.value.canAdmin = !!_.canAdmin, m.value.canWrite = !!_.canWrite), c.value = !1, A.setTitle(o("delegate.edit")), A.setBreadcrumbs([
          { title: o("nav.home"), to: "/" },
          { title: o("nav.identity") },
          { title: o("delegate.title"), to: "/id/delegate" },
          { title: m.value.receiver || o("delegate.edit") }
        ]);
      } else
        A.setTitle(o("delegate.new")), A.setBreadcrumbs([
          { title: o("nav.home"), to: "/" },
          { title: o("nav.identity") },
          { title: o("delegate.title"), to: "/id/delegate" },
          { title: o("delegate.new") }
        ]);
      le();
    });
    async function q() {
      const { valid: f } = await s.value.validate();
      if (!f) return;
      D.value = !0;
      const _ = {
        receiver: m.value.receiver,
        receiverType: m.value.receiverType,
        name: m.value.name,
        type: m.value.type,
        canAdmin: m.value.canAdmin,
        canWrite: m.value.canWrite
      };
      V.value ? await w.put("rest/security/delegate", { id: Number(b.params.id), ..._ }) : await w.post("rest/security/delegate", _), D.value = !1, B(), E.push("/id/delegate");
    }
    async function d() {
      L.value = !0, await w.del(`rest/security/delegate/${b.params.id}`), L.value = !1, x.value = !1, B(), E.push("/id/delegate");
    }
    return (f, _) => {
      const y = i("v-skeleton-loader"), p = i("v-text-field"), Q = i("v-select"), G = i("v-checkbox"), O = i("v-form"), J = i("v-card-text"), X = i("v-icon"), R = i("v-btn"), Y = i("v-spacer"), T = i("v-card-actions"), u = i("v-card"), W = i("v-card-title"), K = i("v-dialog");
      return k(), ne("div", null, [
        te("div", ct, [
          te("h1", mt, n(V.value ? l(o)("delegate.edit") : l(o)("delegate.new")), 1)
        ]),
        c.value ? (k(), h(y, {
          key: 0,
          type: "card, actions",
          "max-width": "700",
          class: "mb-4"
        })) : U("", !0),
        c.value ? U("", !0) : (k(), h(u, {
          key: 1,
          class: "edit-card"
        }, {
          default: t(() => [
            e(J, null, {
              default: t(() => [
                e(O, {
                  ref_key: "formRef",
                  ref: s,
                  onSubmit: oe(q, ["prevent"])
                }, {
                  default: t(() => [
                    e(p, {
                      modelValue: m.value.receiver,
                      "onUpdate:modelValue": _[0] || (_[0] = (C) => m.value.receiver = C),
                      label: l(o)("delegate.receiver"),
                      rules: [$.required],
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules"]),
                    e(Q, {
                      modelValue: m.value.receiverType,
                      "onUpdate:modelValue": _[1] || (_[1] = (C) => m.value.receiverType = C),
                      label: l(o)("delegate.receiverType"),
                      items: S,
                      rules: [$.required],
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules"]),
                    e(p, {
                      modelValue: m.value.name,
                      "onUpdate:modelValue": _[2] || (_[2] = (C) => m.value.name = C),
                      label: l(o)("delegate.resource"),
                      rules: [$.required],
                      hint: l(o)("delegate.resourceHint"),
                      "persistent-hint": "",
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules", "hint"]),
                    e(Q, {
                      modelValue: m.value.type,
                      "onUpdate:modelValue": _[3] || (_[3] = (C) => m.value.type = C),
                      label: l(o)("delegate.type"),
                      items: P,
                      rules: [$.required],
                      variant: "outlined",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label", "rules"]),
                    e(G, {
                      modelValue: m.value.canAdmin,
                      "onUpdate:modelValue": _[4] || (_[4] = (C) => m.value.canAdmin = C),
                      label: l(o)("delegate.admin"),
                      "hide-details": "",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"]),
                    e(G, {
                      modelValue: m.value.canWrite,
                      "onUpdate:modelValue": _[5] || (_[5] = (C) => m.value.canWrite = C),
                      label: l(o)("delegate.write"),
                      "hide-details": "",
                      class: "mb-2"
                    }, null, 8, ["modelValue", "label"])
                  ]),
                  _: 1
                }, 512)
              ]),
              _: 1
            }),
            e(T, null, {
              default: t(() => [
                V.value ? (k(), h(R, {
                  key: 0,
                  color: "error",
                  variant: "tonal",
                  onClick: _[6] || (_[6] = (C) => x.value = !0)
                }, {
                  default: t(() => [
                    e(X, { start: "" }, {
                      default: t(() => [..._[11] || (_[11] = [
                        a("mdi-delete", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(o)("common.delete")), 1)
                  ]),
                  _: 1
                })) : U("", !0),
                e(Y),
                e(R, {
                  variant: "text",
                  onClick: _[7] || (_[7] = (C) => l(E).push("/id/delegate"))
                }, {
                  default: t(() => [
                    a(n(l(o)("common.cancel")), 1)
                  ]),
                  _: 1
                }),
                e(R, {
                  color: "primary",
                  variant: "elevated",
                  loading: D.value,
                  onClick: q
                }, {
                  default: t(() => [
                    e(X, { start: "" }, {
                      default: t(() => [..._[12] || (_[12] = [
                        a("mdi-content-save", -1)
                      ])]),
                      _: 1
                    }),
                    a(" " + n(l(o)("common.save")), 1)
                  ]),
                  _: 1
                }, 8, ["loading"])
              ]),
              _: 1
            })
          ]),
          _: 1
        })),
        e(K, {
          modelValue: x.value,
          "onUpdate:modelValue": _[9] || (_[9] = (C) => x.value = C),
          "max-width": "400"
        }, {
          default: t(() => [
            e(u, null, {
              default: t(() => [
                e(W, null, {
                  default: t(() => [
                    a(n(l(o)("delegate.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(J, null, {
                  default: t(() => [
                    a(n(l(o)("delegate.deleteConfirm", { name: m.value.receiver })), 1)
                  ]),
                  _: 1
                }),
                e(T, null, {
                  default: t(() => [
                    e(Y),
                    e(R, {
                      variant: "text",
                      onClick: _[8] || (_[8] = (C) => x.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(R, {
                      color: "error",
                      variant: "elevated",
                      loading: L.value,
                      onClick: d
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.delete")), 1)
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
        e(K, {
          modelValue: l(F),
          "onUpdate:modelValue": _[10] || (_[10] = (C) => be(F) ? F.value = C : null),
          "max-width": "400"
        }, {
          default: t(() => [
            e(u, null, {
              default: t(() => [
                e(W, null, {
                  default: t(() => [
                    a(n(l(o)("common.unsavedTitle")), 1)
                  ]),
                  _: 1
                }),
                e(J, null, {
                  default: t(() => [
                    a(n(l(o)("common.unsavedMsg")), 1)
                  ]),
                  _: 1
                }),
                e(T, null, {
                  default: t(() => [
                    e(Y),
                    e(R, {
                      variant: "text",
                      onClick: l(ee)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.cancel")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"]),
                    e(R, {
                      color: "warning",
                      variant: "elevated",
                      onClick: l(I)
                    }, {
                      default: t(() => [
                        a(n(l(o)("common.discard")), 1)
                      ]),
                      _: 1
                    }, 8, ["onClick"])
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
}, Se = /* @__PURE__ */ ce(vt, [["__scopeId", "data-v-7867a090"]]), pt = { class: "d-flex flex-wrap align-center mb-4 ga-2" }, ft = { class: "text-h4" }, _t = {
  __name: "ContainerScopeView",
  setup(z) {
    const b = ue(), E = re(), w = _e(), v = de().t, o = g("group"), s = [
      { id: 1, name: "Department", locked: !1 },
      { id: 2, name: "Team", locked: !1 },
      { id: 3, name: "Project", locked: !0 }
    ], c = [
      { id: 1, name: "Organization", locked: !1 },
      { id: 2, name: "Business Unit", locked: !0 }
    ], D = g([]), L = g(0), x = g(!1), V = g(null), S = g(!1), P = ie(() => [
      { title: v("common.name"), key: "name", sortable: !0 },
      { title: v("common.status"), key: "locked", sortable: !1, width: "80px" },
      { title: "", key: "actions", sortable: !1, width: "100px", align: "end" }
    ]), m = g(null), M = g(!1), F = g(null), I = g({ name: "" }), ee = g(!1), B = g(!1), le = g(null), $ = g(!1), q = { required: (G) => !!G || v("common.required") };
    async function d() {
      x.value = !0, V.value = null;
      try {
        const G = await b.get(`rest/service/id/container-scope/${o.value}`);
        G && !G.code ? (D.value = Array.isArray(G) ? G : G.data || [], L.value = D.value.length, S.value = !1) : (S.value = !0, w.clear(), D.value = o.value === "group" ? s : c, L.value = D.value.length);
      } catch {
        S.value = !0, w.clear(), D.value = o.value === "group" ? s : c, L.value = D.value.length;
      }
      x.value = !1;
    }
    Me(o, () => {
      d();
    });
    function f() {
      F.value = null, I.value = { name: "" }, M.value = !0;
    }
    function _(G) {
      F.value = G, I.value = { name: G.name }, M.value = !0;
    }
    function y(G) {
      le.value = G, B.value = !0;
    }
    async function p() {
      var J;
      const { valid: G } = await m.value.validate();
      if (!G) return;
      if (S.value) {
        w.push({ message: v("containerScope.demoSave"), status: 0 }), M.value = !1;
        return;
      }
      ee.value = !0;
      const O = { name: I.value.name };
      (J = F.value) != null && J.id ? await b.put(`rest/service/id/container-scope/${o.value}`, { id: F.value.id, ...O }) : await b.post(`rest/service/id/container-scope/${o.value}`, O), ee.value = !1, M.value = !1, d();
    }
    async function Q() {
      if (S.value) {
        w.push({ message: v("containerScope.demoDelete"), status: 0 }), B.value = !1;
        return;
      }
      $.value = !0, await b.del(`rest/service/id/container-scope/${o.value}/${le.value.id}`), $.value = !1, B.value = !1, d();
    }
    return se(() => {
      E.setTitle(v("containerScope.title")), E.setBreadcrumbs([
        { title: v("nav.home"), to: "/" },
        { title: v("nav.identity") },
        { title: v("containerScope.title") }
      ]), d();
    }), (G, O) => {
      const J = i("v-spacer"), X = i("v-btn"), R = i("v-tab"), Y = i("v-tabs"), T = i("v-alert"), u = i("v-skeleton-loader"), W = i("v-icon"), K = i("v-data-table"), C = i("v-card-title"), N = i("v-text-field"), r = i("v-form"), j = i("v-card-text"), ae = i("v-card-actions"), pe = i("v-card"), fe = i("v-dialog");
      return k(), ne("div", null, [
        te("div", pt, [
          te("h1", ft, n(l(v)("containerScope.title")), 1),
          e(J),
          e(X, {
            color: "primary",
            "prepend-icon": "mdi-plus",
            onClick: f
          }, {
            default: t(() => [
              a(n(l(v)("containerScope.new")), 1)
            ]),
            _: 1
          })
        ]),
        e(Y, {
          modelValue: o.value,
          "onUpdate:modelValue": O[0] || (O[0] = (H) => o.value = H),
          class: "mb-4"
        }, {
          default: t(() => [
            e(R, { value: "group" }, {
              default: t(() => [
                a(n(l(v)("nav.groups")), 1)
              ]),
              _: 1
            }),
            e(R, { value: "company" }, {
              default: t(() => [
                a(n(l(v)("nav.companies")), 1)
              ]),
              _: 1
            })
          ]),
          _: 1
        }, 8, ["modelValue"]),
        V.value ? (k(), h(T, {
          key: 0,
          type: "warning",
          variant: "tonal",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(v)("containerScope.noProvider")), 1)
          ]),
          _: 1
        })) : U("", !0),
        S.value ? (k(), h(T, {
          key: 1,
          type: "info",
          variant: "tonal",
          density: "compact",
          class: "mb-4"
        }, {
          default: t(() => [
            a(n(l(v)("containerScope.demoMode")), 1)
          ]),
          _: 1
        })) : U("", !0),
        x.value && D.value.length === 0 ? (k(), h(u, {
          key: 2,
          type: "table-heading, table-row@5",
          class: "mb-4"
        })) : U("", !0),
        V.value ? U("", !0) : ye((k(), h(K, {
          key: 3,
          headers: P.value,
          items: D.value,
          loading: x.value,
          "item-value": "id",
          hover: "",
          "onClick:row": O[1] || (O[1] = (H, { item: me }) => _(me))
        }, {
          "item.locked": t(({ item: H }) => [
            H.locked ? (k(), h(W, {
              key: 0,
              color: "warning",
              size: "small"
            }, {
              default: t(() => [...O[7] || (O[7] = [
                a("mdi-lock", -1)
              ])]),
              _: 1
            })) : U("", !0)
          ]),
          "item.actions": t(({ item: H }) => [
            e(X, {
              icon: "",
              size: "small",
              variant: "text",
              onClick: oe((me) => _(H), ["stop"])
            }, {
              default: t(() => [
                e(W, { size: "small" }, {
                  default: t(() => [...O[8] || (O[8] = [
                    a("mdi-pencil", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick"]),
            e(X, {
              icon: "",
              size: "small",
              variant: "text",
              color: "error",
              onClick: oe((me) => y(H), ["stop"]),
              disabled: H.locked
            }, {
              default: t(() => [
                e(W, { size: "small" }, {
                  default: t(() => [...O[9] || (O[9] = [
                    a("mdi-delete", -1)
                  ])]),
                  _: 1
                })
              ]),
              _: 1
            }, 8, ["onClick", "disabled"])
          ]),
          _: 1
        }, 8, ["headers", "items", "loading"])), [
          [ke, D.value.length > 0 || !x.value]
        ]),
        e(fe, {
          modelValue: M.value,
          "onUpdate:modelValue": O[4] || (O[4] = (H) => M.value = H),
          "max-width": "500"
        }, {
          default: t(() => [
            e(pe, null, {
              default: t(() => [
                e(C, null, {
                  default: t(() => {
                    var H;
                    return [
                      a(n((H = F.value) != null && H.id ? l(v)("containerScope.edit") : l(v)("containerScope.new")), 1)
                    ];
                  }),
                  _: 1
                }),
                e(j, null, {
                  default: t(() => [
                    e(r, {
                      ref_key: "formRef",
                      ref: m,
                      onSubmit: oe(p, ["prevent"])
                    }, {
                      default: t(() => [
                        e(N, {
                          modelValue: I.value.name,
                          "onUpdate:modelValue": O[2] || (O[2] = (H) => I.value.name = H),
                          label: l(v)("common.name"),
                          rules: [q.required],
                          variant: "outlined",
                          class: "mb-2"
                        }, null, 8, ["modelValue", "label", "rules"])
                      ]),
                      _: 1
                    }, 512)
                  ]),
                  _: 1
                }),
                e(ae, null, {
                  default: t(() => [
                    e(J),
                    e(X, {
                      variant: "text",
                      onClick: O[3] || (O[3] = (H) => M.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(X, {
                      color: "primary",
                      variant: "elevated",
                      loading: ee.value,
                      onClick: p
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.save")), 1)
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
        e(fe, {
          modelValue: B.value,
          "onUpdate:modelValue": O[6] || (O[6] = (H) => B.value = H),
          "max-width": "400"
        }, {
          default: t(() => [
            e(pe, null, {
              default: t(() => [
                e(C, null, {
                  default: t(() => [
                    a(n(l(v)("containerScope.deleteTitle")), 1)
                  ]),
                  _: 1
                }),
                e(j, null, {
                  default: t(() => {
                    var H;
                    return [
                      a(n(l(v)("containerScope.deleteConfirm", { name: (H = le.value) == null ? void 0 : H.name })), 1)
                    ];
                  }),
                  _: 1
                }),
                e(ae, null, {
                  default: t(() => [
                    e(J),
                    e(X, {
                      variant: "text",
                      onClick: O[5] || (O[5] = (H) => B.value = !1)
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.cancel")), 1)
                      ]),
                      _: 1
                    }),
                    e(X, {
                      color: "error",
                      variant: "elevated",
                      loading: $.value,
                      onClick: Q
                    }, {
                      default: t(() => [
                        a(n(l(v)("common.delete")), 1)
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
}, he = "/rest/", ge = {
  requireAgreement(z) {
    return !z || !z["security-agreement"];
  },
  async acceptAgreement(z) {
    if (!(await fetch(he + "system/setting/security-agreement/1", {
      method: "POST",
      credentials: "include"
    })).ok) throw new Error("Failed to accept agreement");
    return z && (z["security-agreement"] = !0), !0;
  },
  scheduleUpload(z, b, E, w) {
    const A = setInterval(
      () => ge._syncUpload(z, b, E, w, A),
      1e3
    );
    return A;
  },
  async _syncUpload(z, b, E, w, A) {
    try {
      const v = await fetch(he + z + "/" + b + "/status", { credentials: "include" });
      if (!v.ok) return;
      const o = await v.json();
      w == null || w(o), o.end && (clearInterval(A), await ge._finishUpload(z, b, E, w));
    } catch (v) {
      console.error("[plugin:id] upload sync error", v);
    }
  },
  async _finishUpload(z, b, E, w) {
    try {
      const A = await fetch(he + z + "/" + b, { credentials: "include" });
      if (!A.ok) return;
      const v = await A.json();
      w == null || w({ ...v.status, finished: !0, errors: v.entries }), E == null || E(v);
    } catch (A) {
      console.error("[plugin:id] upload result error", A);
    }
  }
};
if (typeof document < "u") {
  const z = "ligoj-plugin-id-css";
  if (!document.getElementById(z)) {
    const b = document.createElement("link");
    b.id = z, b.rel = "stylesheet", b.href = new URL(
      /* @vite-ignore */
      "./index.css",
      import.meta.url
    ).href, document.head.appendChild(b);
  }
}
const gt = {
  requireAgreement: ge.requireAgreement,
  acceptAgreement: ge.acceptAgreement,
  scheduleUpload: ge.scheduleUpload
}, Te = [
  { path: "/id/user", name: "id-user", component: je },
  { path: "/id/user/new", name: "id-user-new", component: De },
  { path: "/id/user/:id", name: "id-user-edit", component: De },
  { path: "/id/group", name: "id-group", component: Ke },
  { path: "/id/group/new", name: "id-group-new", component: $e },
  { path: "/id/group/:id", name: "id-group-edit", component: $e },
  { path: "/id/company", name: "id-company", component: at },
  { path: "/id/company/new", name: "id-company-new", component: Ue },
  { path: "/id/company/:id", name: "id-company-edit", component: Ue },
  { path: "/id/delegate", name: "id-delegate", component: dt },
  { path: "/id/delegate/new", name: "id-delegate-new", component: Se },
  { path: "/id/delegate/:id", name: "id-delegate-edit", component: Se },
  { path: "/id/container-scope", name: "id-container-scope", component: _t }
], wt = {
  id: "id",
  label: "Identity",
  component: Re,
  routes: Te,
  install({ router: z }) {
    for (const b of Te)
      z.addRoute(b);
  },
  feature(z, ...b) {
    const E = gt[z];
    if (!E) throw new Error(`Plugin "id" has no feature "${z}"`);
    return E(...b);
  },
  service: ge,
  meta: { icon: "mdi-account-group", color: "blue-darken-3" }
};
export {
  wt as default,
  ge as service
};
