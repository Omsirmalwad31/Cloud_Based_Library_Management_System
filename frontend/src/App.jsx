import React, { useEffect, useMemo, useState } from "react";
import { api } from "./services/api.js";

const storedSession = () => {
  try {
    return JSON.parse(localStorage.getItem("library-session")) ?? null;
  } catch {
    return null;
  }
};

function Login({ onLogin }) {
  const [email, setEmail] = useState("student@library.local");
  const [password, setPassword] = useState("library123");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const session = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      onLogin(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-layout">
      <section className="login-story">
        <div className="brand-mark">L</div>
        <span className="eyebrow">YOUR CAMPUS LIBRARY</span>
        <h1>A quieter place<br />to find your next idea.</h1>
        <p>Browse the collection, keep track of your reading, and make every visit count.</p>
        <div className="story-note"><span>01 / 03</span><span>Read. Return. Repeat.</span></div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <span className="eyebrow">WELCOME BACK</span>
          <h2>Sign in to your library</h2>
          <p className="muted">Use your library account to continue.</p>
          <form onSubmit={submit} className="form-stack">
            <label>Email address<input type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
            <label>Password<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
            {error && <p className="error-message" role="alert">{error}</p>}
            <button className="button button-dark button-wide" disabled={loading}>{loading ? "Signing in…" : "Sign in"} <span aria-hidden="true">→</span></button>
          </form>
          <p className="demo-hint">Demo accounts: <strong>student@library.local</strong> or <strong>admin@library.local</strong><br />Password: <strong>library123</strong></p>
        </div>
      </section>
    </main>
  );
}

function App() {
  const [session, setSession] = useState(storedSession);
  const [books, setBooks] = useState([]);
  const [loans, setLoans] = useState([]);
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [showAddBook, setShowAddBook] = useState(false);

  const activeLoans = useMemo(() => loans.filter((loan) => !loan.returned), [loans]);

  function signIn(value) {
    localStorage.setItem("library-session", JSON.stringify(value));
    setSession(value);
  }

  function signOut() {
    localStorage.removeItem("library-session");
    setSession(null);
    setBooks([]);
    setLoans([]);
  }

  async function loadData(currentSession = session, search = query) {
    if (!currentSession) return;
    setError("");
    try {
      const encoded = encodeURIComponent(search.trim());
      const [bookList, loanList] = await Promise.all([
        api(`/api/books?search=${encoded}`, { token: currentSession.token }),
        api("/api/loans/me", { token: currentSession.token }),
      ]);
      setBooks(bookList);
      setLoans(loanList);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  useEffect(() => {
    if (session) loadData(session, "");
  }, [session]);

  async function issueBook(bookId) {
    setBusyId(bookId);
    setNotice("");
    setError("");
    try {
      await api(`/api/loans/${bookId}`, { method: "POST", token: session.token });
      setNotice("Book added to your loans. It is due in 14 days.");
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function returnBook(loanId) {
    setBusyId(loanId);
    setNotice("");
    setError("");
    try {
      await api(`/api/loans/${loanId}/return`, { method: "PUT", token: session.token });
      setNotice("Book returned. Thanks for taking care of it.");
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function addBook(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setError("");
    setNotice("");
    try {
      await api("/api/books", {
        method: "POST",
        token: session.token,
        body: JSON.stringify({
          title: form.get("title"),
          author: form.get("author"),
          isbn: form.get("isbn"),
          category: form.get("category"),
          copies: Number(form.get("copies")),
        }),
      });
      formElement.reset();
      setShowAddBook(false);
      setNotice("Book added to the collection.");
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  if (!session) return <Login onLogin={signIn} />;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#"><span className="brand-mark small">L</span><span>folio<span className="brand-dot">.</span></span></a>
        <div className="side-label">LIBRARY</div>
        <a className="nav-link active" href="#collection"><span>▦</span> Collection</a>
        <a className="nav-link" href="#my-books"><span>▤</span> My books <span className="nav-count">{activeLoans.length}</span></a>
        <div className="sidebar-bottom">
          <div className="profile-avatar">{session.email.charAt(0).toUpperCase()}</div>
          <div className="profile-copy"><strong>{session.email.split("@")[0]}</strong><span>{session.role.toLowerCase()}</span></div>
          <button className="icon-button signout" onClick={signOut} aria-label="Sign out" title="Sign out">↗</button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar"><span>Monday, your next chapter awaits.</span><span className="open-status"><i /> LIBRARY OPEN</span></header>
        <section className="welcome-row">
          <div><span className="eyebrow">THE COLLECTION</span><h1>Find your next<br className="mobile-break" /> <em>great read.</em></h1><p>Thoughtful reads for curious minds.</p></div>
          <div className="welcome-art" aria-hidden="true"><span className="art-circle" /><span className="art-book one" /><span className="art-book two" /><span className="art-book three" /></div>
        </section>

        <section className="stats-grid" aria-label="Library summary">
          <article className="stat-card"><span>AVAILABLE TITLES</span><strong>{books.length.toString().padStart(2, "0")}</strong><small>In this collection</small></article>
          <article className="stat-card"><span>ON YOUR SHELF</span><strong>{activeLoans.length.toString().padStart(2, "0")}</strong><small>Currently borrowed</small></article>
          <article className="stat-card stat-accent"><span>OVERDUE FINES</span><strong>₹{activeLoans.reduce((sum, loan) => sum + loan.fineRupees, 0)}</strong><small>₹5 per day after due date</small></article>
        </section>

        {(notice || error) && <div className={`notice ${error ? "notice-error" : ""}`} role={error ? "alert" : "status"}>{error || notice}<button onClick={() => { setError(""); setNotice(""); }} aria-label="Dismiss">×</button></div>}

        <section className="collection-section" id="collection">
          <div className="section-heading"><div><span className="eyebrow">A LITTLE SOMETHING FOR EVERYONE</span><h2>Browse the collection</h2></div>
            {session.role !== "STUDENT" && <button className="button button-dark" onClick={() => setShowAddBook(!showAddBook)}>{showAddBook ? "Close" : "+ Add a book"}</button>}
          </div>
          {showAddBook && <form className="add-book-form" onSubmit={addBook}>
            <input name="title" placeholder="Book title" required />
            <input name="author" placeholder="Author" required />
            <input name="isbn" placeholder="ISBN" required />
            <input name="category" placeholder="Category" required />
            <input name="copies" type="number" min="1" defaultValue="1" required />
            <button className="button button-dark">Save book</button>
          </form>}
          <form className="search-bar" onSubmit={(event) => { event.preventDefault(); loadData(session, query); }}>
            <span aria-hidden="true">⌕</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by title, author, or subject..." aria-label="Search books" /><button>Search</button>
          </form>
          <div className="book-grid">
            {books.map((book, index) => <article className="book-card" key={book.id}>
              <div className={`book-cover cover-${index % 4}`}><span className="cover-deco" /><span className="cover-category">{book.category}</span><span className="cover-title">{book.title}</span><span className="cover-author">{book.author}</span></div>
              <div className="book-info"><div><h3>{book.title}</h3><p>{book.author}</p></div><span className={`availability ${book.availableCopies ? "" : "unavailable"}`}>{book.availableCopies ? `${book.availableCopies} available` : "All borrowed"}</span></div>
              <button className="borrow-button" disabled={!book.availableCopies || busyId === book.id} onClick={() => issueBook(book.id)}>{busyId === book.id ? "Working…" : book.availableCopies ? "Borrow this book →" : "Currently unavailable"}</button>
            </article>)}
            {!books.length && <div className="empty-state">No books found. Try another search.</div>}
          </div>
        </section>

        <section className="loans-section" id="my-books">
          <div className="section-heading"><div><span className="eyebrow">YOUR READING LIST</span><h2>My books</h2></div><span className="loan-total">{loans.length} total</span></div>
          {!loans.length ? <p className="muted empty-loans">You haven't borrowed anything yet. Your next read is waiting above.</p> :
            <div className="loan-list">{loans.map((loan) => <article className="loan-row" key={loan.id}><div className="loan-icon">▤</div><div className="loan-copy"><strong>{loan.title}</strong><span>Borrowed {loan.issueDate} · Due {loan.dueDate}</span></div><div className="loan-fine">{loan.fineRupees > 0 ? `₹${loan.fineRupees} fine` : loan.returned ? "Returned" : "On time"}</div>{loan.returned ? <span className="returned-label">Returned ✓</span> : <button className="return-button" disabled={busyId === loan.id} onClick={() => returnBook(loan.id)}>{busyId === loan.id ? "Working…" : "Return"}</button>}</article>)}</div>}
        </section>
        <footer className="footer">MADE FOR THE LOVE OF READING <span>·</span> FOLIO LIBRARY</footer>
      </main>
    </div>
  );
}

export default App;
