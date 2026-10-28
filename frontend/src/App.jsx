import React, { useEffect, useMemo, useState } from "react";
import { api } from "./services/api.js";
import "./library-features.css";

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
        <h1>
          A quieter place
          <br />
          to find your next idea.
        </h1>
        <p>
          Browse the collection, keep track of your reading, and make every
          visit count.
        </p>
        <div className="story-note">
          <span>01 / 03</span>
          <span>Read. Return. Repeat.</span>
        </div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <span className="eyebrow">WELCOME BACK</span>
          <h2>Sign in to your library</h2>
          <p className="muted">Use your library account to continue.</p>
          <form onSubmit={submit} className="form-stack">
            <label>
              Email address
              <input
                type="email"
                autoComplete="username"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </label>
            {error && (
              <p className="error-message" role="alert">
                {error}
              </p>
            )}
            <button
              className="button button-dark button-wide"
              disabled={loading}
            >
              {loading ? "Signing in…" : "Sign in"}{" "}
              <span aria-hidden="true">→</span>
            </button>
          </form>
          <p className="demo-hint">
            Demo accounts: <strong>student@library.local</strong> or{" "}
            <strong>admin@library.local</strong>
            <br />
            Password: <strong>library123</strong>
          </p>
        </div>
      </section>
    </main>
  );
}

function App() {
  const [session, setSession] = useState(storedSession);
  const [books, setBooks] = useState([]);
  const [loans, setLoans] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [adminLoans, setAdminLoans] = useState([]);
  const [allReservations, setAllReservations] = useState([]);
  const [query, setQuery] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [showAddBook, setShowAddBook] = useState(false);
  const [editingBookId, setEditingBookId] = useState(null);

  const activeLoans = useMemo(
    () => loans.filter((loan) => !loan.returned),
    [loans],
  );

  function signIn(value) {
    localStorage.setItem("library-session", JSON.stringify(value));
    setSession(value);
  }

  function signOut() {
    localStorage.removeItem("library-session");
    setSession(null);
    setBooks([]);
    setLoans([]);
    setReservations([]);
    setAdminLoans([]);
    setAllReservations([]);
  }

  async function loadData(currentSession = session, search = query) {
    if (!currentSession) return;
    setError("");
    try {
      const encoded = encodeURIComponent(search.trim());
      const isStaff = currentSession.role !== "STUDENT";
      const [
        bookList,
        loanList,
        reservationList,
        staffLoans,
        staffReservations,
      ] = await Promise.all([
        api(`/api/books?search=${encoded}`, { token: currentSession.token }),
        api("/api/loans/me", { token: currentSession.token }),
        api("/api/reservations/me", { token: currentSession.token }),
        isStaff
          ? api("/api/loans", { token: currentSession.token })
          : Promise.resolve([]),
        isStaff
          ? api("/api/reservations", { token: currentSession.token })
          : Promise.resolve([]),
      ]);
      setBooks(bookList);
      setLoans(loanList);
      setReservations(reservationList);
      setAdminLoans(staffLoans);
      setAllReservations(staffReservations);
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
      await api(`/api/loans/${bookId}`, {
        method: "POST",
        token: session.token,
      });
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
      await api(`/api/loans/${loanId}/return`, {
        method: "PUT",
        token: session.token,
      });
      setNotice("Book returned. Thanks for taking care of it.");
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function renewLoan(loanId) {
    setBusyId(loanId);
    setNotice("");
    setError("");
    try {
      await api(`/api/loans/${loanId}/renew`, {
        method: "PUT",
        token: session.token,
      });
      setNotice("Loan renewed for another 14 days.");
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

  async function deleteBook(book) {
    if (!window.confirm(`Delete "${book.title}" from the collection?`)) return;
    setBusyId(book.id);
    setError("");
    setNotice("");
    try {
      await api(`/api/books/${book.id}`, {
        method: "DELETE",
        token: session.token,
      });
      setNotice(`"${book.title}" was deleted.`);
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function updateBook(event, book) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusyId(book.id);
    setError("");
    setNotice("");
    try {
      await api(`/api/books/${book.id}`, {
        method: "PUT",
        token: session.token,
        body: JSON.stringify({
          title: form.get("title"),
          author: form.get("author"),
          isbn: form.get("isbn"),
          category: form.get("category"),
          copies: Number(form.get("copies")),
        }),
      });
      setEditingBookId(null);
      setNotice("Book details updated.");
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function reserveBook(book) {
    setBusyId(book.id);
    setError("");
    setNotice("");
    try {
      await api(`/api/reservations/${book.id}`, {
        method: "POST",
        token: session.token,
      });
      setNotice(`You are in the reservation queue for "${book.title}".`);
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function claimReservation(reservation) {
    setBusyId(reservation.id);
    setError("");
    setNotice("");
    try {
      await api(`/api/reservations/${reservation.id}/claim`, {
        method: "POST",
        token: session.token,
      });
      setNotice(`"${reservation.title}" has been added to your loans.`);
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  async function cancelReservation(reservation) {
    setBusyId(reservation.id);
    setError("");
    setNotice("");
    try {
      await api(`/api/reservations/${reservation.id}`, {
        method: "DELETE",
        token: session.token,
      });
      setNotice(`Reservation for "${reservation.title}" cancelled.`);
      await loadData(session);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusyId(null);
    }
  }

  if (!session) return <Login onLogin={signIn} />;

  const isStaff = session.role !== "STUDENT";
  const activeReservations = reservations.filter(
    (reservation) =>
      reservation.status === "WAITING" || reservation.status === "READY",
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#">
          <span className="brand-mark small">L</span>
          <span>
            folio<span className="brand-dot">.</span>
          </span>
        </a>
        <div className="side-label">LIBRARY</div>
        <a className="nav-link active" href="#collection">
          <span>▦</span> Collection
        </a>
        <a className="nav-link" href="#my-books">
          <span>▤</span> My books{" "}
          <span className="nav-count">{activeLoans.length}</span>
        </a>
        {isStaff && (
          <a className="nav-link" href="#staff-loans">
            <span>▤</span> Staff loans
          </a>
        )}
        <div className="sidebar-bottom">
          <div className="profile-avatar">
            {session.email.charAt(0).toUpperCase()}
          </div>
          <div className="profile-copy">
            <strong>{session.email.split("@")[0]}</strong>
            <span>{session.role.toLowerCase()}</span>
          </div>
          <button
            className="icon-button signout"
            onClick={signOut}
            aria-label="Sign out"
            title="Sign out"
          >
            ↗
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <span>Monday, your next chapter awaits.</span>
          <span className="open-status">
            <i /> LIBRARY OPEN
          </span>
        </header>
        <section className="welcome-row">
          <div>
            <span className="eyebrow">THE COLLECTION</span>
            <h1>
              Find your next
              <br className="mobile-break" /> <em>great read.</em>
            </h1>
            <p>Thoughtful reads for curious minds.</p>
          </div>
          <div className="welcome-art" aria-hidden="true">
            <span className="art-circle" />
            <span className="art-book one" />
            <span className="art-book two" />
            <span className="art-book three" />
          </div>
        </section>

        <section className="stats-grid" aria-label="Library summary">
          <article className="stat-card">
            <span>AVAILABLE TITLES</span>
            <strong>{books.length.toString().padStart(2, "0")}</strong>
            <small>In this collection</small>
          </article>
          <article className="stat-card">
            <span>ON YOUR SHELF</span>
            <strong>{activeLoans.length.toString().padStart(2, "0")}</strong>
            <small>Currently borrowed</small>
          </article>
          <article className="stat-card stat-accent">
            <span>OVERDUE FINES</span>
            <strong>
              ₹{activeLoans.reduce((sum, loan) => sum + loan.fineRupees, 0)}
            </strong>
            <small>₹5 per day after due date</small>
          </article>
        </section>

        {(notice || error) && (
          <div
            className={`notice ${error ? "notice-error" : ""}`}
            role={error ? "alert" : "status"}
          >
            {error || notice}
            <button
              onClick={() => {
                setError("");
                setNotice("");
              }}
              aria-label="Dismiss"
            >
              ×
            </button>
          </div>
        )}

        <section className="collection-section" id="collection">
          <div className="section-heading">
            <div>
              <span className="eyebrow">A LITTLE SOMETHING FOR EVERYONE</span>
              <h2>Browse the collection</h2>
            </div>
            {session.role !== "STUDENT" && (
              <button
                className="button button-dark"
                onClick={() => setShowAddBook(!showAddBook)}
              >
                {showAddBook ? "Close" : "+ Add a book"}
              </button>
            )}
          </div>
          {showAddBook && (
            <form className="add-book-form" onSubmit={addBook}>
              <input name="title" placeholder="Book title" required />
              <input name="author" placeholder="Author" required />
              <input name="isbn" placeholder="ISBN" required />
              <input name="category" placeholder="Category" required />
              <input
                name="copies"
                type="number"
                min="1"
                defaultValue="1"
                required
              />
              <button className="button button-dark">Save book</button>
            </form>
          )}
          <form
            className="search-bar"
            onSubmit={(event) => {
              event.preventDefault();
              loadData(session, query);
            }}
          >
            <span aria-hidden="true">⌕</span>
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by title, author, or subject..."
              aria-label="Search books"
            />
            <button>Search</button>
          </form>
          <div className="book-grid">
            {books.map((book, index) => (
              <article className="book-card" key={book.id}>
                <div className={`book-cover cover-${index % 4}`}>
                  <span className="cover-deco" />
                  <span className="cover-category">{book.category}</span>
                  <span className="cover-title">{book.title}</span>
                  <span className="cover-author">{book.author}</span>
                </div>
                <div className="book-info">
                  <div>
                    <h3>{book.title}</h3>
                    <p>{book.author}</p>
                  </div>
                  <span
                    className={`availability ${book.availableCopies ? "" : "unavailable"}`}
                  >
                    {book.availableCopies
                      ? `${book.availableCopies} available`
                      : "All borrowed"}
                  </span>
                </div>
                {editingBookId === book.id ? (
                  <form
                    className="book-edit-form"
                    onSubmit={(event) => updateBook(event, book)}
                  >
                    <input
                      name="title"
                      defaultValue={book.title}
                      aria-label="Book title"
                      required
                    />
                    <input
                      name="author"
                      defaultValue={book.author}
                      aria-label="Author"
                      required
                    />
                    <input
                      name="isbn"
                      defaultValue={book.isbn}
                      aria-label="ISBN"
                      required
                    />
                    <input
                      name="category"
                      defaultValue={book.category}
                      aria-label="Category"
                      required
                    />
                    <label>
                      Copies{" "}
                      <input
                        name="copies"
                        type="number"
                        min={book.totalCopies - book.availableCopies}
                        defaultValue={book.totalCopies}
                        required
                      />
                    </label>
                    <button
                      className="button button-dark"
                      disabled={busyId === book.id}
                    >
                      {busyId === book.id ? "Saving…" : "Save changes"}
                    </button>
                    <button
                      type="button"
                      className="return-button"
                      onClick={() => setEditingBookId(null)}
                    >
                      Cancel
                    </button>
                  </form>
                ) : (
                  <>
                    {book.availableCopies > 0 ? (
                      <button
                        className="borrow-button"
                        disabled={busyId === book.id}
                        onClick={() => issueBook(book.id)}
                      >
                        {busyId === book.id ? "Working…" : "Borrow this book →"}
                      </button>
                    ) : (
                      (() => {
                        const reservation = activeReservations.find(
                          (item) => item.bookId === book.id,
                        );
                        if (reservation?.status === "READY")
                          return (
                            <button
                              className="borrow-button"
                              disabled={busyId === reservation.id}
                              onClick={() => claimReservation(reservation)}
                            >
                              {busyId === reservation.id
                                ? "Working…"
                                : "Claim reserved copy →"}
                            </button>
                          );
                        if (reservation?.status === "WAITING")
                          return (
                            <button
                              className="borrow-button"
                              disabled={busyId === reservation.id}
                              onClick={() => cancelReservation(reservation)}
                            >
                              {busyId === reservation.id
                                ? "Working…"
                                : "In queue · Cancel reservation"}
                            </button>
                          );
                        return (
                          <button
                            className="borrow-button"
                            disabled={busyId === book.id}
                            onClick={() => reserveBook(book)}
                          >
                            {busyId === book.id
                              ? "Working…"
                              : "Reserve this book →"}
                          </button>
                        );
                      })()
                    )}
                    {isStaff && (
                      <button
                        className="borrow-button"
                        onClick={() => setEditingBookId(book.id)}
                      >
                        Edit book
                      </button>
                    )}
                    {session.role === "ADMIN" && (
                      <button
                        className="borrow-button"
                        disabled={busyId === book.id}
                        onClick={() => deleteBook(book)}
                      >
                        {busyId === book.id ? "Working…" : "Delete book"}
                      </button>
                    )}
                  </>
                )}
              </article>
            ))}
            {!books.length && (
              <div className="empty-state">
                No books found. Try another search.
              </div>
            )}
          </div>
        </section>

        <section className="loans-section" id="my-books">
          <div className="section-heading">
            <div>
              <span className="eyebrow">YOUR READING LIST</span>
              <h2>My books</h2>
            </div>
            <span className="loan-total">{loans.length} total</span>
          </div>
          {!loans.length ? (
            <p className="muted empty-loans">
              You haven't borrowed anything yet. Your next read is waiting
              above.
            </p>
          ) : (
            <div className="loan-list">
              {loans.map((loan) => (
                <article className="loan-row" key={loan.id}>
                  <div className="loan-icon">▤</div>
                  <div className="loan-copy">
                    <strong>{loan.title}</strong>
                    <span>
                      Borrowed {loan.issueDate} · Due {loan.dueDate}
                    </span>
                  </div>
                  <div className="loan-fine">
                    {loan.fineRupees > 0
                      ? `₹${loan.fineRupees} fine`
                      : loan.returned
                        ? "Returned"
                        : "On time"}
                  </div>
                  {loan.returned ? (
                    <span className="returned-label">Returned ✓</span>
                  ) : (
                    <div className="loan-actions">
                      {loan.renewalAvailable && (
                        <button
                          className="return-button"
                          disabled={busyId === loan.id}
                          onClick={() => renewLoan(loan.id)}
                        >
                          Renew 14 days
                        </button>
                      )}
                      <button
                        className="return-button"
                        disabled={busyId === loan.id}
                        onClick={() => returnBook(loan.id)}
                      >
                        {busyId === loan.id ? "Working…" : "Return"}
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
        {activeReservations.length > 0 && (
          <section className="loans-section" id="my-reservations">
            <div className="section-heading">
              <div>
                <span className="eyebrow">WAITLIST</span>
                <h2>My reservations</h2>
              </div>
            </div>
            <div className="loan-list">
              {activeReservations.map((reservation) => (
                <article className="loan-row" key={reservation.id}>
                  <div className="loan-icon">▤</div>
                  <div className="loan-copy">
                    <strong>{reservation.title}</strong>
                    <span>
                      {reservation.status === "READY"
                        ? "A copy is ready for you to claim."
                        : "Waiting for a copy to become available."}
                    </span>
                  </div>
                  {reservation.status === "READY" && (
                    <button
                      className="return-button"
                      disabled={busyId === reservation.id}
                      onClick={() => claimReservation(reservation)}
                    >
                      Claim copy
                    </button>
                  )}
                  <button
                    className="return-button"
                    disabled={busyId === reservation.id}
                    onClick={() => cancelReservation(reservation)}
                  >
                    Cancel
                  </button>
                </article>
              ))}
            </div>
          </section>
        )}
        {isStaff && (
          <section className="loans-section" id="staff-loans">
            <div className="section-heading">
              <div>
                <span className="eyebrow">LIBRARY OPERATIONS</span>
                <h2>All loans</h2>
              </div>
              <span className="loan-total">
                {adminLoans.filter((loan) => !loan.returned).length} active
              </span>
            </div>
            {!adminLoans.length ? (
              <p className="muted empty-loans">No loan records yet.</p>
            ) : (
              <div className="loan-list">
                {adminLoans.map((loan) => (
                  <article className="loan-row staff-loan-row" key={loan.id}>
                    <div className="loan-icon">▤</div>
                    <div className="loan-copy">
                      <strong>{loan.title}</strong>
                      <span>
                        {loan.borrowerEmail} · Borrowed {loan.issueDate} · Due{" "}
                        {loan.dueDate}
                      </span>
                    </div>
                    <div className="loan-fine">
                      {loan.returned
                        ? "Returned"
                        : loan.fineRupees > 0
                          ? `Overdue · ₹${loan.fineRupees}`
                          : "On time"}
                    </div>
                    {!loan.returned && (
                      <button
                        className="return-button"
                        disabled={busyId === loan.id}
                        onClick={() => returnBook(loan.id)}
                      >
                        Record return
                      </button>
                    )}
                  </article>
                ))}
              </div>
            )}
            {allReservations.some(
              (reservation) =>
                reservation.status === "WAITING" ||
                reservation.status === "READY",
            ) && (
              <div className="staff-reservations">
                <h3>Reservation queue</h3>
                {allReservations
                  .filter(
                    (reservation) =>
                      reservation.status === "WAITING" ||
                      reservation.status === "READY",
                  )
                  .map((reservation) => (
                    <p key={reservation.id}>
                      <strong>{reservation.title}</strong> ·{" "}
                      {reservation.borrowerEmail} ·{" "}
                      {reservation.status.toLowerCase()}
                    </p>
                  ))}
              </div>
            )}
          </section>
        )}
        <footer className="footer">
          MADE FOR THE LOVE OF READING <span>·</span> FOLIO LIBRARY
        </footer>
      </main>
    </div>
  );
}

export default App;
