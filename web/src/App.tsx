import { useEffect, useMemo, useState, type FormEvent } from "react";
import {
  authenticate,
  type AuthSession,
} from "./auth";
import {
  ApiError,
  getAnalyticsSummary,
  getOrderHistory,
  getNotifications,
  placeOrder,
  searchFoods,
  sendChatMessage,
  type AnalyticsSummary,
  type ChatMessage,
  type DailyAnalytics,
  type Notification,
  type OrderReceipt,
  type Product,
} from "./api";

type CartLine = { product: Product; quantity: number };
type Category = "All" | "Mains" | "Bowls" | "Sides" | "Drinks";

const samples: Product[] = [
  { id: "demo-crispy-chicken", name: "Crispy chicken bowl", description: "Crunchy chicken, herby rice, pickled greens & lemon tahini.", price: 13.5 },
  { id: "demo-salmon-bowl", name: "Miso-glazed salmon", description: "Wild salmon, sesame rice, cucumber & a little chilli crunch.", price: 16 },
  { id: "demo-harvest-bowl", name: "Garden harvest bowl", description: "Roasted seasonal veg, grains, avocado & green goddess dressing.", price: 12.25 },
  { id: "demo-crispy-potatoes", name: "Crispy rosemary potatoes", description: "Golden little potatoes, rosemary salt & whipped garlic dip.", price: 5.5 },
  { id: "demo-lemonade", name: "Fresh pink lemonade", description: "Squeezed lemons, a touch of berry & just enough sparkle.", price: 4 },
  { id: "demo-choco-tart", name: "Chocolate hazelnut tart", description: "Silky dark chocolate, toasted hazelnut & flaky sea salt.", price: 7.5 },
];

const categories: Category[] = ["All", "Mains", "Bowls", "Sides", "Drinks"];
const money = (value: number) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);

function classify(product: Product): { category: Category; emoji: string; style: string; label: string } {
  const text = `${product.name} ${product.description}`.toLowerCase();
  if (/drink|juice|coffee|tea|lemonade|water/.test(text)) return { category: "Drinks", emoji: "🍋", style: "lemon", label: "Sips & refreshers" };
  if (/side|potato|fries|salad|bread|soup/.test(text)) return { category: "Sides", emoji: "🥗", style: "green", label: "On the side" };
  if (/bowl|rice|grain/.test(text)) return { category: "Bowls", emoji: /salmon|fish/.test(text) ? "🍣" : "🥙", style: "peach", label: "Made for a moment" };
  if (/dessert|cake|chocolate|tart|cookie|sweet/.test(text)) return { category: "Sides", emoji: "🍰", style: "berry", label: "Something sweet" };
  return { category: "Mains", emoji: /fish|salmon/.test(text) ? "🐟" : /chicken/.test(text) ? "🍗" : "🍽️", style: "gold", label: "Kitchen favorite" };
}

function Header({
  cartCount,
  isSignedIn,
  onAuth,
  onCart,
  onSignOut,
  onOrders,
  onNotifications,
  onAnalytics,
  onAssistant,
  isAdmin,
}: {
  cartCount: number;
  isSignedIn: boolean;
  onAuth: (mode: "login" | "register") => void;
  onCart: () => void;
  onSignOut: () => void;
  onOrders: () => void;
  onNotifications: () => void;
  onAnalytics: () => void;
  onAssistant: () => void;
  isAdmin: boolean;
}) {
  return (
    <header className="site-header">
      <a className="brand" href="#home" aria-label="FoodFusion home"><span className="brand-mark">f.</span><span>foodfusion<span className="brand-dot">.</span></span></a>
      <nav className="main-nav" aria-label="Main navigation"><a className="nav-active" href="#menu">Menu</a><a href="#story">Our story</a></nav>
      <div className="header-actions">
        <button className="text-button assistant-link" onClick={onAssistant}>Food assistant</button>
        {isSignedIn ? <><button className="text-button account-button" onClick={onOrders}>My orders</button><button className="text-button account-button" onClick={onNotifications}>Notifications</button>{isAdmin && <button className="text-button account-button" onClick={onAnalytics}>Analytics</button>}<button className="text-button account-button" onClick={onSignOut}>Sign out</button></> : <button className="text-button account-button" onClick={() => onAuth("login")}>Sign in</button>}
        <button className="cart-button" onClick={onCart} aria-label={`Open basket, ${cartCount} items`}><span className="bag-icon">♧</span><span className="cart-label">Basket</span><span className="cart-count">{cartCount}</span></button>
      </div>
    </header>
  );
}

function App() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [products, setProducts] = useState<Product[]>(samples);
  const [isSampleMenu, setIsSampleMenu] = useState(true);
  const [menuError, setMenuError] = useState("");
  const [loadingMenu, setLoadingMenu] = useState(false);
  const [menuReload, setMenuReload] = useState(0);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [query, setQuery] = useState("");
  const [activeCategory, setActiveCategory] = useState<Category>("All");
  const [authMode, setAuthMode] = useState<"login" | "register" | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState("");
  const [cartOpen, setCartOpen] = useState(false);
  const [ordersOpen, setOrdersOpen] = useState(false);
  const [orders, setOrders] = useState<OrderReceipt[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [notificationsError, setNotificationsError] = useState("");
  const [analyticsOpen, setAnalyticsOpen] = useState(false);
  const [analytics, setAnalytics] = useState<{ summary: AnalyticsSummary; daily: DailyAnalytics[] } | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);
  const [analyticsError, setAnalyticsError] = useState("");
  const [placingOrder, setPlacingOrder] = useState(false);
  const [checkoutMessage, setCheckoutMessage] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [assistantInput, setAssistantInput] = useState("");
  const [assistantMessages, setAssistantMessages] = useState<ChatMessage[]>([]);
  const [assistantBusy, setAssistantBusy] = useState(false);
  const [assistantError, setAssistantError] = useState("");

  useEffect(() => {
    let cancelled = false;
    const timer = window.setTimeout(() => {
      setLoadingMenu(true);
      setMenuError("");
      searchFoods(query, session)
      .then((items) => {
        if (cancelled) return;
        setProducts(items);
        setIsSampleMenu(false);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setMenuError(error instanceof Error ? error.message : "The menu service could not be reached.");
        setProducts(samples);
        setIsSampleMenu(true);
      })
      .finally(() => { if (!cancelled) setLoadingMenu(false); });
    }, 250);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [menuReload, query, session]);

  useEffect(() => {
    if (!session || !ordersOpen) return;
    let cancelled = false;
    setOrdersLoading(true);
    setOrdersError("");
    getOrderHistory(session)
      .then((history) => { if (!cancelled) setOrders(history); })
      .catch((error: unknown) => {
        if (!cancelled) setOrdersError(error instanceof Error ? error.message : "Could not load your orders.");
      })
      .finally(() => { if (!cancelled) setOrdersLoading(false); });
    return () => { cancelled = true; };
  }, [ordersOpen, session]);

  useEffect(() => {
    if (!session || !notificationsOpen) return;
    let cancelled = false;
    setNotificationsLoading(true);
    setNotificationsError("");
    getNotifications(session)
      .then((items) => { if (!cancelled) setNotifications(items); })
      .catch((error: unknown) => {
        if (!cancelled) setNotificationsError(error instanceof Error ? error.message : "Could not load notifications.");
      })
      .finally(() => { if (!cancelled) setNotificationsLoading(false); });
    return () => { cancelled = true; };
  }, [notificationsOpen, session]);

  useEffect(() => {
    if (!session || !analyticsOpen || !session.roles.includes("ADMIN")) return;
    let cancelled = false;
    setAnalyticsLoading(true);
    setAnalyticsError("");
    getAnalyticsSummary(session)
      .then((result) => { if (!cancelled) setAnalytics(result); })
      .catch((error: unknown) => {
        if (!cancelled) setAnalyticsError(error instanceof Error ? error.message : "Could not load analytics.");
      })
      .finally(() => { if (!cancelled) setAnalyticsLoading(false); });
    return () => { cancelled = true; };
  }, [analyticsOpen, session]);

  const filteredProducts = useMemo(() => products.filter((product) => {
    const kind = classify(product);
    const matchesCategory = activeCategory === "All" || kind.category === activeCategory;
    return matchesCategory;
  }), [activeCategory, products]);

  const cartCount = cart.reduce((sum, line) => sum + line.quantity, 0);
  const total = cart.reduce((sum, line) => sum + line.product.price * line.quantity, 0);

  const addToCart = (product: Product) => {
    setCart((current) => {
      const existing = current.find((line) => line.product.id === product.id);
      return existing
        ? current.map((line) => line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line)
        : [...current, { product, quantity: 1 }];
    });
    setCheckoutMessage("");
  };

  const updateQuantity = (id: string, amount: number) => setCart((current) =>
    current.map((line) => line.product.id === id ? { ...line, quantity: line.quantity + amount } : line).filter((line) => line.quantity > 0));

  const handleAuth = async (mode: "login" | "register") => {
    setAuthBusy(true);
    setAuthError("");
    try {
      const authenticated = await authenticate(mode, { username: username.trim(), ...(mode === "register" ? { email: email.trim() } : {}), password });
      setSession(authenticated);
      setAuthMode(null);
      setPassword("");
      setAuthError("");
    }
    catch (error) {
      setAuthError(error instanceof Error ? error.message : "Could not start sign-in.");
    } finally {
      setAuthBusy(false);
    }
  };

  const submitOrder = async () => {
    if (!session || isSampleMenu || cart.length === 0) return;
    setPlacingOrder(true);
    setCheckoutMessage("");
    try {
      await placeOrder(cart.map(({ product, quantity }) => ({ id: product.id, price: product.price, quantity })), session);
      setCart([]);
      setCheckoutMessage("Your order has been placed.");
      try {
        setOrders(await getOrderHistory(session));
      } catch {
        setCheckoutMessage("Your order was placed. Order history could not be refreshed just now.");
      }
    } catch (error) {
      setCheckoutMessage(error instanceof Error ? error.message : "We couldn't place your order. Please try again.");
    } finally {
      setPlacingOrder(false);
    }
  };

  const submitAssistantMessage = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const message = assistantInput.trim();
    if (!message || !session || assistantBusy) return;
    setAssistantInput("");
    setAssistantError("");
    setAssistantMessages((current) => [...current, { role: "user", content: message }]);
    setAssistantBusy(true);
    try {
      const answer = await sendChatMessage(message, session);
      setAssistantMessages((current) => [...current, { role: "assistant", content: answer }]);
    } catch (error) {
      setAssistantError(error instanceof Error ? error.message : "The assistant could not reply.");
    } finally {
      setAssistantBusy(false);
    }
  };

  return (
    <div id="home" className="app-shell">
      <Header
        cartCount={cartCount}
        isSignedIn={!!session}
        isAdmin={!!session?.roles.includes("ADMIN")}
        onAuth={(mode) => { setAuthMode(mode); setAuthError(""); }}
        onCart={() => setCartOpen(true)}
        onSignOut={() => { setSession(null); setProducts(samples); setIsSampleMenu(true); setMenuError(""); setCart([]); setOrders([]); setNotifications([]); setAnalytics(null); setAssistantMessages([]); }}
        onOrders={() => setOrdersOpen(true)}
        onNotifications={() => setNotificationsOpen(true)}
        onAnalytics={() => setAnalyticsOpen(true)}
        onAssistant={() => { setAssistantOpen(true); setAssistantError(""); }}
      />

      {menuError && <div className="service-alert" role="status"><span className="alert-icon">!</span><span><strong>Menu connection issue.</strong> {menuError} You can still explore the sample menu below.</span><button onClick={() => setMenuReload((current) => current + 1)}>Try again</button></div>}

      <main>
        <section className="hero">
          <div className="hero-copy">
            <span className="eyebrow"><span className="eyebrow-line" />GOOD FOOD, GOOD MOOD</span>
            <h1>A little joy,<br />in every <em>bite.</em></h1>
            <p>Freshly made, thoughtfully sourced, and always worth slowing down for. Your next favorite is just a few clicks away.</p>
            <div className="hero-actions"><a className="primary-button" href="#menu">Explore the menu <span aria-hidden="true">↗</span></a><span className="delivery-note"><span className="delivery-icon">◷</span>Made fresh, delivered happy</span></div>
            <div className="social-proof"><div className="avatar-stack"><span>👩🏽</span><span>👨🏻</span><span>👩🏼</span><span>👨🏾</span></div><span><strong>4.9/5</strong> from food lovers like you</span></div>
          </div>
          <div className="hero-art" aria-label="Illustration of a fresh bowl">
            <div className="art-sun" />
            <div className="art-caption"><span>Fresh from<br />our kitchen</span><span className="caption-spark">✳</span></div>
            <div className="bowl-shadow" />
            <div className="bowl-plate"><div className="bowl-rim"><div className="bowl-food">
              <span className="food-leaf leaf-one">✿</span><span className="food-leaf leaf-two">✿</span><span className="food-tomato">●</span><span className="food-avocado">◕</span><span className="food-grain">···</span><span className="food-herb">❋</span><span className="food-seed">▪</span>
            </div></div></div>
            <div className="art-stamp"><span>made with<br /><strong>love</strong></span><span>♥</span></div>
            <div className="art-sparkle sparkle-one">✦</div><div className="art-sparkle sparkle-two">✧</div>
          </div>
          <div className="hero-bottom"><span>01 / 03</span><span className="hero-bottom-line" /><span>Something good is cooking</span></div>
        </section>

        <section className="perks" aria-label="Why FoodFusion"><div className="perk"><span className="perk-icon green-icon">✿</span><span><strong>Fresh, always</strong><small>Real ingredients, no shortcuts</small></span></div><span className="perk-divider" /><div className="perk"><span className="perk-icon peach-icon">♧</span><span><strong>Made with care</strong><small>A little love in every order</small></span></div><span className="perk-divider" /><div className="perk"><span className="perk-icon gold-icon">⌁</span><span><strong>Easy & joyful</strong><small>Good food, without the fuss</small></span></div></section>

        <section id="menu" className="menu-section">
          <div className="section-heading"><div><span className="eyebrow"><span className="eyebrow-line" />THE GOOD STUFF</span><h2>Made for <em>your mood.</em></h2><p>What sounds good today?</p></div><div className="menu-meta"><span className="open-indicator" />{loadingMenu ? "Getting the menu…" : isSampleMenu ? "A little menu inspiration" : `${products.length} kitchen favorites`}</div></div>
          {isSampleMenu && <div className="sample-notice"><span>✦</span> Showing a sample menu while live search is unavailable. Sign in to place an order.</div>}
          <div className="menu-controls"><div className="category-tabs" role="tablist" aria-label="Filter menu by category">{categories.map((category) => <button key={category} role="tab" aria-selected={activeCategory === category} className={activeCategory === category ? "category-tab selected" : "category-tab"} onClick={() => setActiveCategory(category)}>{category}</button>)}</div><label className="search-box"><span aria-hidden="true">⌕</span><input aria-label="Search menu" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Find something lovely..." />{query && <button aria-label="Clear search" onClick={() => setQuery("")}>×</button>}</label></div>
          <div className="product-grid">
            {filteredProducts.map((product, index) => {
              const visual = classify(product);
              return <article className={`product-card ${index === 1 ? "card-featured" : ""}`} key={product.id}>
                <div className={`product-art ${visual.style}`}><span className="food-emoji" aria-hidden="true">{visual.emoji}</span><span className="dish-label">{visual.label}</span><button className="favorite-button" aria-label={`Add ${product.name} to favorites`} onClick={(event) => event.currentTarget.classList.toggle("favorited")}>♡</button><span className="dish-spark">✦</span></div>
                <div className="product-info"><div className="product-title-row"><h3>{product.name}</h3><strong>{money(product.price)}</strong></div><p>{product.description || "Made fresh in our kitchen with ingredients we love."}</p><div className="product-bottom"><span className="rating"><span>★</span> 4.9 <i>·</i> 20 min</span><button className="add-button" onClick={() => addToCart(product)} aria-label={`Add ${product.name} to basket`}>+</button></div></div>
              </article>;
            })}
            {filteredProducts.length === 0 && <div className="empty-menu"><span>⌕</span><h3>Nothing on the menu just yet</h3><p>Try another search or category.</p><button className="text-button" onClick={() => { setQuery(""); setActiveCategory("All"); }}>Show everything</button></div>}
          </div>
        </section>

        <section id="story" className="story-band"><div className="story-decoration">✳</div><div><span className="eyebrow">A LITTLE ABOUT US</span><h2>Good food should<br /><em>feel good, too.</em></h2></div><p>We believe the best meals are made with care: for the ingredients, for the people who grow them, and for the people around your table.</p><a href="#menu" className="story-link">A taste of our kitchen <span>↗</span></a></section>
      </main>

      <footer className="site-footer"><a className="brand footer-brand" href="#home"><span className="brand-mark">f.</span><span>foodfusion<span className="brand-dot">.</span></span></a><span>Made with a little extra love. <span className="footer-heart">♥</span></span><span>© 2026 FoodFusion Kitchen</span></footer>

      {cartOpen && <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setCartOpen(false); }}><aside className="side-panel" role="dialog" aria-modal="true" aria-labelledby="basket-title"><div className="panel-heading"><div><span className="eyebrow">YOUR LITTLE FEAST</span><h2 id="basket-title">Your basket<span className="brand-dot">.</span></h2></div><button className="close-button" onClick={() => setCartOpen(false)} aria-label="Close basket">×</button></div>{checkoutMessage && <div className={checkoutMessage.startsWith("Your order") ? "checkout-message success" : "checkout-message"}>{checkoutMessage}</div>}
        {cart.length === 0 ? <div className="empty-cart"><span>♧</span><h3>A good meal starts here.</h3><p>Your basket is waiting for something delicious.</p><button className="primary-button" onClick={() => setCartOpen(false)}>Explore the menu</button></div> : <><div className="cart-lines">{cart.map(({ product, quantity }) => <div className="cart-line" key={product.id}><div className={`mini-art ${classify(product).style}`}>{classify(product).emoji}</div><div className="line-detail"><strong>{product.name}</strong><span>{money(product.price)} each</span><div className="quantity-control"><button onClick={() => updateQuantity(product.id, -1)} aria-label={`Remove one ${product.name}`}>−</button><span>{quantity}</span><button onClick={() => updateQuantity(product.id, 1)} aria-label={`Add one ${product.name}`}>+</button></div></div><strong className="line-total">{money(product.price * quantity)}</strong></div>)}</div><div className="cart-summary"><div><span>Subtotal</span><strong>{money(total)}</strong></div><small>Taxes and delivery are calculated at checkout.</small>{isSampleMenu ? <button className="checkout-button" disabled>Live menu required to place an order</button> : !session ? <button className="checkout-button" onClick={() => setAuthMode("login")}>Sign in to order <span>→</span></button> : <button className="checkout-button" onClick={submitOrder} disabled={placingOrder}>{placingOrder ? "Sending your order…" : "Place order"} <span>→</span></button>}</div></>}</aside></div>}

      {authMode && (
        <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setAuthMode(null); }}>
          <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
            <button className="close-button auth-close" onClick={() => setAuthMode(null)} aria-label="Close sign-in">×</button>
            <div className="auth-mark">f.</div>
            <span className="eyebrow">PULL UP A CHAIR</span>
            <h2 id="auth-title">{authMode === "login" ? "Good to see you." : "Come on in."}</h2>
            <p>{authMode === "login" ? "Sign in to see your orders and order something lovely." : "Create an account and let the good meals begin."}</p>
            {authError && <div className="auth-error" role="alert">{authError}</div>}
            <form onSubmit={(event) => { event.preventDefault(); void handleAuth(authMode); }}>
              <label className="auth-field">
                {authMode === "login" ? "Email" : "Name"}
                <input
                  type={authMode === "login" ? "email" : "text"}
                  autoComplete={authMode === "login" ? "email" : "name"}
                  required
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                />
              </label>
              {authMode === "register" && (
                <label className="auth-field">
                  Email
                  <input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
                </label>
              )}
              <label className="auth-field">
                Password
                <input
                  type="password"
                  autoComplete={authMode === "login" ? "current-password" : "new-password"}
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </label>
              <button type="submit" className="checkout-button auth-continue" disabled={authBusy}>
                {authBusy ? "Connecting securely…" : authMode === "login" ? "Sign in" : "Create account"} <span>→</span>
              </button>
            </form>
            <div className="auth-divider"><span />SECURE SIGN-IN<span /></div>
            <p className="auth-footnote">Your password is sent securely to FoodFusion and is never stored in this app.</p>
            <button className="auth-switch" onClick={() => { setAuthMode(authMode === "login" ? "register" : "login"); setAuthError(""); setPassword(""); }}>
              {authMode === "login" ? "New around here? Create an account" : "Already have an account? Sign in"}
            </button>
          </section>
        </div>
      )}

      {ordersOpen && <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setOrdersOpen(false); }}><section className="orders-dialog" role="dialog" aria-modal="true" aria-labelledby="orders-title"><div className="panel-heading"><div><span className="eyebrow">FROM OUR KITCHEN TO YOU</span><h2 id="orders-title">Your orders<span className="brand-dot">.</span></h2></div><button className="close-button" onClick={() => setOrdersOpen(false)} aria-label="Close orders">×</button></div>{ordersError && <div className="auth-error" role="alert">{ordersError}</div>}{ordersLoading ? <div className="history-loading">Loading your orders…</div> : orders.length === 0 ? <div className="empty-orders"><span>✿</span><h3>No orders just yet.</h3><p>Your next favorite meal is waiting on the menu.</p><button className="primary-button" onClick={() => setOrdersOpen(false)}>Find your next favorite</button></div> : <div className="order-list">{orders.map((order) => <article className="order-card" key={order.id}><div className="order-card-top"><div><span className="order-ref">ORDER {order.id}</span><small>{order.placedAt ? new Date(order.placedAt).toLocaleString() : "Date unavailable"}</small></div><span className="order-status">{order.status}</span></div><div className="order-items">{order.items.map((item, index) => <span key={`${order.id}-${index}`}>{item.quantity} × {item.name}</span>)}</div><div className="order-card-bottom"><span>{order.items.length} item{order.items.length === 1 ? "" : "s"}</span><strong>{money(order.total)}</strong></div></article>)}</div>}</section></div>}

      {notificationsOpen && <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setNotificationsOpen(false); }}><section className="orders-dialog" role="dialog" aria-modal="true" aria-labelledby="notifications-title"><div className="panel-heading"><div><span className="eyebrow">ORDER UPDATES</span><h2 id="notifications-title">Notifications<span className="brand-dot">.</span></h2></div><button className="close-button" onClick={() => setNotificationsOpen(false)} aria-label="Close notifications">×</button></div>{notificationsError && <div className="auth-error" role="alert">{notificationsError}</div>}{notificationsLoading ? <div className="history-loading">Loading notifications…</div> : notifications.length === 0 ? <div className="empty-orders"><span>✿</span><h3>You're all caught up.</h3><p>Updates about your orders will appear here.</p></div> : <div className="order-list">{notifications.map((notification) => <article className="order-card" key={notification.id}><div className="order-card-top"><div><span className="order-ref">ORDER {notification.orderNumber}</span><small>{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : "Date unavailable"}</small></div><span className="order-status">{notification.status}</span></div><div className="order-items"><span>{notification.message}</span></div><div className="order-card-bottom"><span>Order total</span><strong>{money(notification.totalAmount)}</strong></div></article>)}</div>}</section></div>}

      {analyticsOpen && <div className="overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setAnalyticsOpen(false); }}><section className="orders-dialog" role="dialog" aria-modal="true" aria-labelledby="analytics-title"><div className="panel-heading"><div><span className="eyebrow">ADMIN OVERVIEW</span><h2 id="analytics-title">Analytics<span className="brand-dot">.</span></h2></div><button className="close-button" onClick={() => setAnalyticsOpen(false)} aria-label="Close analytics">×</button></div>{analyticsError && <div className="auth-error" role="alert">{analyticsError}</div>}{analyticsLoading ? <div className="history-loading">Loading analytics…</div> : analytics && <><div className="order-card"><div className="order-card-top"><strong>Total orders</strong><strong>{analytics.summary.orderCount}</strong></div><div className="order-card-bottom"><span>Recorded revenue</span><strong>{money(analytics.summary.revenue)}</strong></div></div><div className="order-list">{analytics.daily.map((day) => <article className="order-card" key={day.date}><div className="order-card-top"><strong>{day.date}</strong><span>{day.orderCount} orders</span></div><div className="order-card-bottom"><span>Revenue</span><strong>{money(day.revenue)}</strong></div></article>)}</div></>}</section></div>}
    </div>
  );
}

export default App;
