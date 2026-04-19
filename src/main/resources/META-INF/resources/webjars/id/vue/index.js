import { openBlock as u, createElementBlock as l, createElementVNode as d } from "vue";
const p = (e, r) => {
  const t = e.__vccOpts || e;
  for (const [s, n] of r)
    t[s] = n;
  return t;
}, m = { class: "plugin-id" }, f = {
  __name: "IdPlugin",
  setup(e) {
    return (r, t) => (u(), l("div", m, [...t[0] || (t[0] = [
      d("h2", null, "Identity", -1),
      d("p", null, "Plugin shell — views (users, groups, companies, delegates, container-scopes) move here in slice 3b.", -1)
    ])]));
  }
}, g = /* @__PURE__ */ p(f, [["__scopeId", "data-v-6edf097d"]]), a = "/rest/", o = {
  requireAgreement(e) {
    return !e || !e["security-agreement"];
  },
  async acceptAgreement(e) {
    if (!(await fetch(a + "system/setting/security-agreement/1", {
      method: "POST",
      credentials: "include"
    })).ok) throw new Error("Failed to accept agreement");
    return e && (e["security-agreement"] = !0), !0;
  },
  scheduleUpload(e, r, t, s) {
    const n = setInterval(
      () => o._syncUpload(e, r, t, s, n),
      1e3
    );
    return n;
  },
  async _syncUpload(e, r, t, s, n) {
    try {
      const c = await fetch(a + e + "/" + r + "/status", { credentials: "include" });
      if (!c.ok) return;
      const i = await c.json();
      s == null || s(i), i.end && (clearInterval(n), await o._finishUpload(e, r, t, s));
    } catch (c) {
      console.error("[plugin:id] upload sync error", c);
    }
  },
  async _finishUpload(e, r, t, s) {
    try {
      const n = await fetch(a + e + "/" + r, { credentials: "include" });
      if (!n.ok) return;
      const c = await n.json();
      s == null || s({ ...c.status, finished: !0, errors: c.entries }), t == null || t(c);
    } catch (n) {
      console.error("[plugin:id] upload result error", n);
    }
  }
}, h = {
  requireAgreement: o.requireAgreement,
  acceptAgreement: o.acceptAgreement,
  scheduleUpload: o.scheduleUpload
}, y = {
  id: "id",
  label: "Identity",
  component: g,
  install() {
  },
  feature(e, ...r) {
    const t = h[e];
    if (!t) throw new Error(`Plugin "id" has no feature "${e}"`);
    return t(...r);
  },
  service: o,
  meta: { icon: "mdi-account-group", color: "blue-darken-3" }
};
export {
  y as default,
  o as service
};
