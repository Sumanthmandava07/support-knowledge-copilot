function Header({ title, subtitle }) {

    return (
        <header className="top-header">

            <div>
                <h2>{title}</h2>

                {subtitle && (
                    <p>{subtitle}</p>
                )}
            </div>

            <div className="header-actions">

                <button className="icon-button">
                    🔔
                </button>

                <div className="header-avatar">
                    {(localStorage.getItem("copilotUser") || "S")
                        .charAt(0)
                        .toUpperCase()}
                </div>

            </div>

        </header>
    );
}

export default Header;